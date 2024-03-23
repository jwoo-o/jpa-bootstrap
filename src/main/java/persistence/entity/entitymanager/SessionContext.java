package persistence.entity.entitymanager;

public interface SessionContext {

    EntityManager currentSession();

    void bindSession(EntityManager entityManager);

    void close();
}
