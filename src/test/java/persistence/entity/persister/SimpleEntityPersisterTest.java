package persistence.entity.persister;

import database.DatabaseServer;
import database.H2;
import domain.Order;
import domain.OrderItem;
import domain.Person;
import java.sql.SQLException;
import java.util.stream.IntStream;
import jdbc.EntityRowMapper;
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
import persistence.fixture.OrderFixture;
import persistence.fixture.PersonFixture;
import persistence.sql.ddl.DdlGenerator;
import persistence.sql.dialect.h2.H2Dialect;
import persistence.sql.dml.DmlGenerator;

@DisplayName("SimpleEntityPersister class 의")
class SimpleEntityPersisterTest {

    private DatabaseServer server;
    private JdbcTemplate jdbcTemplate;
    private DdlGenerator ddlGenerator;
    private DmlGenerator dmlGenerator;

    @BeforeEach
    void setUp() throws SQLException {
        server = new H2();
        server.start();

        jdbcTemplate = new JdbcTemplate(server.getConnection());
        ddlGenerator = DdlGenerator.getInstance(H2Dialect.getInstance());
        dmlGenerator = DmlGenerator.getInstance();
        jdbcTemplate.execute(ddlGenerator.generateCreateQuery(Person.class));
        jdbcTemplate.execute(ddlGenerator.generateCreateQuery(Order.class));
        jdbcTemplate.execute(ddlGenerator.generateCreateQuery(OrderItem.class));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute(ddlGenerator.generateDropQuery(Person.class));
        jdbcTemplate.execute(ddlGenerator.generateDropQuery(OrderItem.class));
        jdbcTemplate.execute(ddlGenerator.generateDropQuery(Order.class));
        server.stop();
    }

    @DisplayName("insert 메서드는")
    @Nested
    class Insert {
        @DisplayName("Person Entity를 저장한다.")
        @Test
        void insertTest() {
            EntityPersister<Person> entityPersister = SimpleEntityPersister.of(jdbcTemplate, Person.class);
            //given
            Person person = PersonFixture.createPerson();

            //when
            entityPersister.insert(person);

            //then
            Person foundPerson = findPerson(1L);
            assertAll(
                () -> assertThat(person).isNotNull(),
                () -> assertThat(person.getName()).isEqualTo(foundPerson.getName()),
                () -> assertThat(person.getAge()).isEqualTo(foundPerson.getAge()),
                () -> assertThat(person.getEmail()).isEqualTo(foundPerson.getEmail())
            );
        }

        @DisplayName("Order Entity를 저장한다.")
        @Test
        void insertTest_whenOrder() {
            EntityPersister<Order> entityPersister = SimpleEntityPersister.of(jdbcTemplate, Order.class);

            //given
            Order order = OrderFixture.createOrder();
            order.addOrderItem(OrderFixture.createOrderItem());
            order.addOrderItem(OrderFixture.createOrderItem());
            order.addOrderItem(OrderFixture.createOrderItem());

            //when
            entityPersister.insert(order);

            //then
            Order foundOrder = jdbcTemplate.queryForObject(dmlGenerator.generateSelectQuery(Order.class, 1L),
                resultSet -> new EntityRowMapper<>(Order.class).mapRow(resultSet));

            assertAll(
                () -> assertThat(foundOrder).isEqualTo(order),
                () -> assertThat(foundOrder.getId()).isEqualTo(order.getId()),
                () -> assertThat(foundOrder.getOrderNumber()).isEqualTo(order.getOrderNumber())
            );
        }
    }

    @DisplayName("update 메서드는")
    @Nested
    class Update {
        @DisplayName("Person Entity를 수정한다.")
        @Test
        void updateTest() {
            //given
            EntityPersister<Person> entityPersister = SimpleEntityPersister.of(jdbcTemplate, Person.class);
            Person person = PersonFixture.createPerson();
            entityPersister.insert(person);
            person = findPerson(1L);
            person.updateName("user2");

            //when
            assertThat(entityPersister.update(person)).isTrue();

            //then
            person = findPerson(1L);
            assertThat(person.getName()).isEqualTo("user2");
        }
    }

    @DisplayName("delete 메서드는")
    @Nested
    class Delete {
        @DisplayName("Entity를 삭제한다.")
        @Test
        void deleteTest() {
            //given
            EntityPersister<Person> entityPersister = SimpleEntityPersister.of(jdbcTemplate, Person.class);
            Person person = PersonFixture.createPerson();
            entityPersister.insert(person);
            person = findPerson(1L);

            //when
            entityPersister.delete(person);

            //then
            assertThatThrownBy(() -> findPerson(1L))
                .isInstanceOf(RuntimeException.class);
        }
    }

    private Person findPerson(long id) {
        return jdbcTemplate.queryForObject(dmlGenerator.generateSelectQuery(Person.class, id),
            resultSet -> new EntityRowMapper<>(Person.class).mapRow(resultSet));
    }
}
