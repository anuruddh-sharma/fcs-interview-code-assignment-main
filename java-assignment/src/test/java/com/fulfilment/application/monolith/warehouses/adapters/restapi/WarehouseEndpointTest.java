package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fulfilment.application.monolith.support.EmbeddedPostgresTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(EmbeddedPostgresTestResource.class)
public class WarehouseEndpointTest {

  @Test
  public void listsActiveSeedWarehouses() {
    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void createsGetsAndArchivesWarehouse() {
    String createdId =
        given()
            .contentType(ContentType.JSON)
            .body(warehouse("MWH-HEL-1", "HELMOND-001", 40, 10))
            .when()
            .post("/warehouse")
            .then()
            .statusCode(201)
            .body(containsString("MWH-HEL-1"), containsString("HELMOND-001"))
            .extract()
            .path("id");
    assertNotNull(createdId);

    given()
        .when()
        .get("/warehouse/" + createdId)
        .then()
        .statusCode(200)
        .body(containsString("MWH-HEL-1"));

    given().when().delete("/warehouse/" + createdId).then().statusCode(204);

    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200)
        .body(not(containsString("MWH-HEL-1")), containsString("MWH.012"));

    given()
        .when()
        .get("/warehouse/" + createdId)
        .then()
        .statusCode(200)
        .body(containsString("MWH-HEL-1"));

    given().when().delete("/warehouse/" + createdId).then().statusCode(404);
  }

  @Test
  public void replacesWarehouseAtTheSameLocationThenRejectsAFullLocation() {
    String createdId =
        given()
            .contentType(ContentType.JSON)
            .body(warehouse("MWH-HEL-2", "HELMOND-001", 20, 5))
            .when()
            .post("/warehouse")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String replacementId =
        given()
            .contentType(ContentType.JSON)
            .body(warehouse("MWH-HEL-2", "HELMOND-001", 25, 5))
            .when()
            .post("/warehouse/MWH-HEL-2/replacement")
            .then()
            .statusCode(200)
            .body(containsString("MWH-HEL-2"), containsString("HELMOND-001"))
            .extract()
            .path("id");
    assertNotNull(replacementId);

    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200)
        .body(containsString("MWH-HEL-2"));

    given()
        .contentType(ContentType.JSON)
        .body(warehouse("MWH-HEL-2", "ZWOLLE-001", 10, 5))
        .when()
        .post("/warehouse/MWH-HEL-2/replacement")
        .then()
        .statusCode(400)
        .body(containsString("Maximum number of warehouses"));

    given()
        .when()
        .get("/warehouse/" + replacementId)
        .then()
        .statusCode(200)
        .body(containsString("HELMOND-001"));

    given().when().delete("/warehouse/" + replacementId).then().statusCode(204);
    given().when().get("/warehouse/" + createdId).then().statusCode(200);
  }

  @Test
  public void rejectsInvalidWarehouseRequests() {
    given()
        .contentType(ContentType.JSON)
        .body(warehouse("MWH-BAD-LOC", "NOWHERE-001", 10, 1))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body(containsString("Location is invalid"));

    given()
        .contentType(ContentType.JSON)
        .body(warehouse("MWH.001", "HELMOND-001", 10, 1))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body(containsString("Business unit code already exists"));

    given()
        .contentType(ContentType.JSON)
        .body(warehouse("MWH-ZWOLLE", "ZWOLLE-001", 10, 1))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body(containsString("Maximum number of warehouses"));

    given()
        .contentType(ContentType.JSON)
        .body(warehouse("MWH-VETSBY", "VETSBY-001", 91, 1))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body(containsString("maximum capacity"));

    given()
        .contentType(ContentType.JSON)
        .body(warehouse("MWH-STOCK", "EINDHOVEN-001", 10, 11))
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body(containsString("cannot handle the informed stock"));

    given().when().get("/warehouse/not-a-number").then().statusCode(400);
    given().when().get("/warehouse/99999").then().statusCode(404);
    given().when().delete("/warehouse/99999").then().statusCode(404);

    given()
        .contentType(ContentType.JSON)
        .body(warehouse("OTHER", "HELMOND-001", 10, 1))
        .when()
        .post("/warehouse/MWH.MISSING/replacement")
        .then()
        .statusCode(400)
        .body(containsString("must reuse the business unit code"));

    given()
        .contentType(ContentType.JSON)
        .body(warehouse("MWH.012", "AMSTERDAM-002", 10, 1))
        .when()
        .post("/warehouse/MWH.012/replacement")
        .then()
        .statusCode(400)
        .body(containsString("must match the stock"));
  }

  private static String warehouse(String code, String location, int capacity, int stock) {
    return "{\"businessUnitCode\":\""
        + code
        + "\",\"location\":\""
        + location
        + "\",\"capacity\":"
        + capacity
        + ",\"stock\":"
        + stock
        + "}";
  }
}
