package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import com.fulfilment.application.monolith.support.EmbeddedPostgresTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(EmbeddedPostgresTestResource.class)
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";
    String name = "PROD" + UUID.randomUUID().toString().substring(0, 8);

    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    Number id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"" + name + "\",\"stock\":2}")
            .when()
            .post(path)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + name + "-EDITED\",\"stock\":3}")
        .when()
        .put(path + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString(name + "-EDITED"));

    given().when().delete(path + "/" + id).then().statusCode(204);

    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString(name)),
            containsString("TONSTAD"),
            containsString("KALLAX"),
            containsString("BESTÅ"));
  }

  @Test
  public void missingProductReturns404() {
    given().when().get("product/99999").then().statusCode(404);
    given().when().delete("product/99999").then().statusCode(404);
  }

  @Test
  public void rejectsInvalidProductRequests() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":5,\"name\":\"WITHID\"}")
        .when()
        .post("product")
        .then()
        .statusCode(422);

    given()
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .put("product/1")
        .then()
        .statusCode(422);
  }
}
