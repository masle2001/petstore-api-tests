package com.example.petstore;

import com.example.petstore.model.Category;
import com.example.petstore.model.Pet;
import com.example.petstore.model.Tag;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class PetApiTests extends BaseTest {

    private long randomPetId() {
        return ThreadLocalRandom.current().nextLong(100000, 999999);
    }

    private Pet buildNewPet(long id, String status) {
        Pet pet = new Pet();
        pet.setId(id);
        pet.setName("test-dog-" + id);

        Category category = new Category(1L, "Dogs");
        pet.setCategory(category);

        pet.setPhotoUrls(Collections.singletonList("https://example.com/photo/" + id));

        Tag tag = new Tag(10L, "test-tag");
        pet.setTags(Collections.singletonList(tag));

        pet.setStatus(status);
        return pet;
    }

    @Test
    @DisplayName("CRUD для Pet: POST → GET → PUT → DELETE")
    void petCrudScenario() {
        long id = randomPetId();
        Pet petToCreate = buildNewPet(id, "available");

        // POST /pet
        given()
                .contentType(ContentType.JSON)
                .body(petToCreate)
                .when()
                .post("/pet")
                .then()
                .statusCode(anyOf(is(200), is(201), is(202)));

        // GET /pet/{petId}
        given()
                .pathParam("petId", id)
                .when()
                .get("/pet/{petId}")
                .then()
                .statusCode(200)
                .body("id", equalTo((int) id))
                .body("name", equalTo(petToCreate.getName()))
                .body("status", equalTo("available"));

        // PUT /pet
        Pet petToUpdate = buildNewPet(id, "sold");

        given()
                .contentType(ContentType.JSON)
                .body(petToUpdate)
                .when()
                .put("/pet")
                .then()
                .statusCode(anyOf(is(200), is(201), is(202)));

        // GET /pet/{petId} после обновления
        given()
                .pathParam("petId", id)
                .when()
                .get("/pet/{petId}")
                .then()
                .statusCode(200)
                .body("status", equalTo("sold"));

        // DELETE /pet/{petId}
        given()
                .pathParam("petId", id)
                .when()
                .delete("/pet/{petId}")
                .then()
                .statusCode(anyOf(is(200), is(204)));

        // GET /pet/{petId} после удаления — ожидаем 404 или 400
        given()
                .pathParam("petId", id)
                .when()
                .get("/pet/{petId}")
                .then()
                .statusCode(anyOf(is(404), is(400)));
    }

    @Test
    @DisplayName("GET /pet/findByStatus с несколькими статусами")
    void findPetsByMultipleStatus() {
        given()
                .queryParam("status", "available", "pending", "sold")
                .when()
                .get("/pet/findByStatus")
                .then()
                .statusCode(200)
                .contentType(anyOf(
                        equalTo(ContentType.JSON.toString()),
                        startsWith("application/json")
                ))
                .body("$", is(not(empty())))
                .body("status", everyItem(isIn(List.of("available", "pending", "sold"))));
    }
}
