package persistence.entity.entitymanager;

import java.io.Closeable;

public class ThreadLocalSessionContext implements SessionContext, Closeable {
    private static final ThreadLocal<EntityManager> SESSION = new ThreadLocal<>();

    private ThreadLocalSessionContext() {
    }
    private static class Holder {
        static final ThreadLocalSessionContext INSTANCE = new ThreadLocalSessionContext();
    }

    public static ThreadLocalSessionContext getInstance() {
        return ThreadLocalSessionContext.Holder.INSTANCE;
    }


    @Override
    public EntityManager currentSession() {
        return SESSION.get();
    }

    @Override
    public void bindSession(EntityManager entityManager) {
        SESSION.set(entityManager);
    }

    @Override
    public void close() {
        SESSION.remove();
    }
}
