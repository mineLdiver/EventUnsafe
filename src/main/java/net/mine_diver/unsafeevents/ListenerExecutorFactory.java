package net.mine_diver.unsafeevents;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.*;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

final class ListenerExecutorFactory {

    private static Class<? extends Consumer<? extends Event>> generateExecutor(Method method, Class<? extends Event> eventType) {
        //noinspection unchecked
        return (Class<? extends Consumer<? extends Event>>)
                UnsafeProvider.theUnsafe.defineAnonymousClass(
                        method.getDeclaringClass(),
                        generateExecutorClass(
                                method,
                                method.getDeclaringClass().getName().replace('.', '/') + "$$PericulosusOcto$ListenerExecutor",
                                eventType
                        ),
                        null
                ).asSubclass(Consumer.class);
    }

    private static byte[] generateExecutorClass(Method m, String name, Class<? extends Event> eventType) {
        final boolean staticMethod = Modifier.isStatic(m.getModifiers());
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(V1_8, ACC_PUBLIC, name, null, "java/lang/Object", new String[] {Type.getInternalName(Consumer.class)});
        if (!staticMethod)
            writer.visitField(ACC_PUBLIC, "instance", "Ljava/lang/Object;", null, null).visitEnd();
        // Generate constructor
        MethodVisitor methodGenerator = writer.visitMethod(ACC_PUBLIC, "<init>", staticMethod ? "()V" : "(Ljava/lang/Object;)V", null, null);
        methodGenerator.visitCode();
        methodGenerator.visitVarInsn(ALOAD, 0);
        methodGenerator.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false); // Invoke the super class (Object) constructor
        if (!staticMethod) {
            methodGenerator.visitVarInsn(ALOAD, 0);
            methodGenerator.visitVarInsn(ALOAD, 1);
            methodGenerator.visitFieldInsn(PUTFIELD, name, "instance", "Ljava/lang/Object;");
        }
        methodGenerator.visitInsn(RETURN);
        methodGenerator.visitMaxs(-1, -1);
        methodGenerator.visitEnd();
        // Generate the execute method
        methodGenerator = writer.visitMethod(ACC_PUBLIC, "accept", "(Ljava/lang/Object;)V", null, null);
        methodGenerator.visitCode();
        if (!staticMethod) {
            methodGenerator.visitVarInsn(ALOAD, 0);
            methodGenerator.visitFieldInsn(GETFIELD, name, "instance", "Ljava/lang/Object;");
            methodGenerator.visitTypeInsn(CHECKCAST, Type.getInternalName(m.getDeclaringClass()));
        }
        methodGenerator.visitVarInsn(ALOAD, 1);
        methodGenerator.visitTypeInsn(CHECKCAST, Type.getInternalName(eventType));
        methodGenerator.visitMethodInsn(staticMethod ? INVOKESTATIC : INVOKEVIRTUAL, Type.getInternalName(m.getDeclaringClass()), m.getName(), Type.getMethodDescriptor(m), m.getDeclaringClass().isInterface());
        if (m.getReturnType() != void.class)
            methodGenerator.visitInsn(POP);
        methodGenerator.visitInsn(RETURN);
        methodGenerator.visitMaxs(-1, -1);
        methodGenerator.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static final ConcurrentMap<Method, Class<? extends Consumer<? extends Event>>> cache = new ConcurrentHashMap<>();

    static <T extends Event> Consumer<T> create(Object target, Method method, Class<T> eventType) {
        //noinspection unchecked
        Class<? extends Consumer<T>> executorClass = (Class<? extends Consumer<T>>) cache.computeIfAbsent(method, method1 -> generateExecutor(method1, eventType));
        try {
            if (Modifier.isStatic(method.getModifiers()))
                return executorClass.newInstance();
            else
                return executorClass.getConstructor(Object.class).newInstance(target);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Unable to initialize " + executorClass, e);
        }
    }
}