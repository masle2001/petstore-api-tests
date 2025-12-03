package com.example.petstore;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseTest {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "https://petstore.swagger.io";
        RestAssured.basePath = "/v2";
        // RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(); // можно включить для дебага
    }
}
