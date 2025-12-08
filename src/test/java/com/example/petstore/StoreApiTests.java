package com.example.petstore;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class StoreApiTests extends BaseTest {

    @Test
    @DisplayName("GET /store/inventory возвращает непустую мапу статусов")
    void getInventory() {
        given()
                .when()
                .get("/store/inventory")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", aMapWithSize(greaterThan(0)))
                .body("available", anyOf(nullValue(), greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("POST → GET → DELETE /store/order (полный цикл заказа)")
    void orderLifecycle() {
        long orderId = 1L; // по спецификации для успешного ответа используются id 1..10

        String orderJson = """
            {
              "id": %d,
              "petId": 123456,
              "quantity": 1,
              "shipDate": "%s",
              "status": "placed",
              "complete": true
            }
            """.formatted(orderId, OffsetDateTime.now().toString());

        // POST /store/order - создаём заказ
        given()
                .contentType(ContentType.JSON)
                .body(orderJson)
                .when()
                .post("/store/order")
                .then()
                .statusCode(200)
                .body("id", equalTo((int) orderId))
                .body("status", equalTo("placed"));

        // GET /store/order/{orderId} - проверяем, что заказ вернулся
        given()
                .pathParam("orderId", orderId)
                .when()
                .get("/store/order/{orderId}")
                .then()
                .statusCode(200)
                .body("id", equalTo((int) orderId));

        // DELETE /store/order/{orderId}
        given()
                .pathParam("orderId", orderId)
                .when()
                .delete("/store/order/{orderId}")
                .then()
                .statusCode(anyOf(is(200), is(204)));

        // Повторный GET — ждём 404 или 400
        given()
                .pathParam("orderId", orderId)
                .when()
                .get("/store/order/{orderId}")
                .then()
                .statusCode(anyOf(is(404), is(400)));
    }
    // NEGATIVE TESTS
    @Test
    @DisplayName("NEGATIVE: GET /store/order/{orderId} с id вне диапазона → 400/404")
    void getOrderWithOutOfRangeId() {
        long badId = 9999;

        given()
                .pathParam("orderId", badId)
                .when()
                .get("/store/order/{orderId}")
                .then()
                .statusCode(anyOf(is(400), is(404)));
    }

    @Test
    @DisplayName("NEGATIVE: DELETE /store/order/{orderId} с отрицательным id → 400")
    void deleteOrderWithNegativeId() {
        long badId = -1;

        given()
                .pathParam("orderId", badId)
                .when()
                .delete("/store/order/{orderId}")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("NEGATIVE: POST /store/order с некорректным телом → 400")
    void createOrderWithInvalidBody() {
        String badOrderJson = """
            {
              "id": -1,
              "petId": -123,
              "quantity": -5,
              "shipDate": "not-a-date",
              "status": "bad-status",
              "complete": true
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(badOrderJson)
                .when()
                .post("/store/order")
                .then()
                .statusCode(400);
    }
}

