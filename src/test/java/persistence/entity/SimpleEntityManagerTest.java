package persistence.entity;

import database.DatabaseServer;
import database.H2;
import domain.Department;
import domain.Employee;
import domain.Order;
import domain.OrderItem;
import domain.Person;
import java.sql.SQLException;
import java.util.List;
import jdbc.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import persistence.entity.entitymanager.SimpleEntityManager;
import persistence.entity.entitymanager.SimpleEntityManagerFactory;
import persistence.entity.binder.AnnotationBinder;
import persistence.entity.entitymanager.EntityManager;
import persistence.fixture.OrderFixture;
import persistence.fixture.PersonFixture;
import persistence.sql.ComponentScanner;
import persistence.sql.ddl.DdlGenerator;
import persistence.sql.dialect.h2.H2Dialect;

@DisplayName("SimpleEntityManager class 의")
class SimpleEntityManagerTest {

    private DatabaseServer server;
    List<Class<?>> entityClass = ComponentScanner.getClasses("domain");

    private JdbcTemplate jdbcTemplate;
    private DdlGenerator ddlGenerator;
    private SimpleEntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    @BeforeEach
    void setUp() throws SQLException {
        server = new H2();
        server.start();

        jdbcTemplate = new JdbcTemplate(server);
        ddlGenerator = DdlGenerator.getInstance(H2Dialect.getInstance());
        entityClass.forEach(clazz -> jdbcTemplate.execute(ddlGenerator.generateCreateQuery(clazz)));
        entityManagerFactory = SimpleEntityManagerFactory.getInstance(AnnotationBinder.bind("domain"), server);
        entityManager = entityManagerFactory.openSession();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute(ddlGenerator.generateDropQuery(Person.class));
        jdbcTemplate.execute(ddlGenerator.generateDropQuery(OrderItem.class));
        jdbcTemplate.execute(ddlGenerator.generateDropQuery(Order.class));
        jdbcTemplate.execute(ddlGenerator.generateDropQuery(Employee.class));
        jdbcTemplate.execute(ddlGenerator.generateDropQuery(Department.class));
        server.stop();
    }

    @DisplayName("persist 메서드는")
    @Nested
    class Persist {

        @DisplayName("Person entity를 저장 한다.")
        @Test
        void persistTest_whenInsert() {
            //given
            Person person = PersonFixture.createPerson();

            //when
            entityManager.persist(person);
            entityManager.flush();

            //then
            Person foundPerson = entityManager.find(person.getClass(), 1L);
            assertAll(
                () -> assertEquals(person.getName(), foundPerson.getName()),
                () -> assertEquals(person.getAge(), foundPerson.getAge()),
                () -> assertEquals(person.getEmail(), foundPerson.getEmail())
            );
        }
    }

    @DisplayName("find 메서드는")
    @Nested
    class Find {

        @DisplayName("Person entity를 검색 할 수 있다.")
        @Test
        void findTest() {
            // given
            Person person = PersonFixture.createPerson();
            entityManager.persist(person);
            entityManager.flush();

            // when
            Person foundPerson = entityManager.find(Person.class, 1L);

            // then
            assertAll(
                () -> assertEquals(person.getName(), foundPerson.getName()),
                () -> assertEquals(person.getAge(), foundPerson.getAge()),
                () -> assertEquals(person.getEmail(), foundPerson.getEmail())
            );
        }

        @DisplayName("같은 Person entity를 두 번 검색하면 캐싱된 entity를 반환한다.")
        @Test
        void findTest_whenFindTwice() {
            // given
            Person person = PersonFixture.createPerson();
            entityManager.persist(person);
            entityManager.flush();

            // when
            Person foundPerson1 = entityManager.find(Person.class, 1L);
            Person foundPerson2 = entityManager.find(Person.class, 1L);

            // then
            assertEquals(foundPerson1, foundPerson2);
        }

        @DisplayName("Order entity를 조회 후 oderItem을 lazy로 조회한다.")
        @Test
        void findTest_whenOrder() throws SQLException {
            // given
            Order order = OrderFixture.createOrder();
            order.addOrderItem(OrderFixture.createOrderItem());
            order.addOrderItem(OrderFixture.createOrderItem());
            order.addOrderItem(OrderFixture.createOrderItem());

            entityManager.persist(order);
            entityManager.flush();

            // when
            EntityManager manager = SimpleEntityManagerFactory.getInstance(AnnotationBinder.bind("domain"), server).openSession();
            Order foundOrder = manager.find(Order.class, 1L);

            // then
            assertAll(
                () -> assertEquals(foundOrder, order),
                () -> assertEquals(foundOrder.getId(), order.getId()),
                () -> assertEquals(foundOrder.getOrderNumber(), order.getOrderNumber()),
                () -> assertEquals(foundOrder.getOrderItems().size(), order.getOrderItems().size())
            );
        }

        @DisplayName("department 조회시 employee도 같이 조회한다.")
        @Test
        void findTest_whenDepartment() throws SQLException {
            // given
            Department department = new Department("IT");
            department.addEmployee(new Employee("user1"));
            department.addEmployee(new Employee("user2"));
            department.addEmployee(new Employee("user3"));

            entityManager.persist(department);
            entityManager.flush();

            // when
            EntityManager manager = SimpleEntityManagerFactory.getInstance(AnnotationBinder.bind("domain"), server).openSession();
            Department foundDepartment = manager.find(Department.class, 1L);

            // then
            assertAll(
                () -> assertEquals(foundDepartment, department),
                () -> assertEquals(foundDepartment.getId(), department.getId()),
                () -> assertEquals(foundDepartment.getName(), department.getName()),
                () -> assertEquals(foundDepartment.getEmployees().size(), department.getEmployees().size())
            );
        }
    }

    @DisplayName("remove 메서드는")
    @Nested
    class Remove {

        @DisplayName("특정 Person을 삭제 할 수 있다.")
        @Test
        void deleteTest() {
            //given
            Person person = PersonFixture.createPerson();
            entityManager.persist(person);
            entityManager.flush();
            Person person1 = entityManager.find(Person.class, 1L);

            //when
            entityManager.remove(person1);
            entityManager.flush();

            //then
            assertThatThrownBy(() -> entityManager.find(Person.class, 1L))
                .isInstanceOf(RuntimeException.class);
        }
    }

    @DisplayName("merge 메서드는")
    @Nested
    class Merge {

        @DisplayName("Person entity를 수정 할 수 있다.")
        @Test
        void mergeTest() {
            //given
            Person person = PersonFixture.createPerson();
            entityManager.persist(person);
            entityManager.flush();
            person = entityManager.find(Person.class, 1L);
            person.updateName("user2");

            //when
            entityManager.merge(person);
            entityManager.flush();

            //then
            person = entityManager.find(Person.class, 1L);
            assertEquals("user2", person.getName());
        }

        @DisplayName("Person entity를 flush를 호출하지 않으면 수정하지 않는다.")
        @Test
        void mergeTest_whenNotUpdate() {
            //given
            Person person = PersonFixture.createPerson();
            entityManager.persist(person);
            entityManager.flush();
            person = entityManager.find(Person.class, 1L);
            person.updateName("user2");

            EntityManager entityManager1 = entityManagerFactory.openSession();
            Person person1 = entityManager1.find(Person.class, 1L);
            //when
            entityManager.merge(person);

            //then
            assertThat(person1.getName()).isNotEqualTo("user2");
        }
    }
}
