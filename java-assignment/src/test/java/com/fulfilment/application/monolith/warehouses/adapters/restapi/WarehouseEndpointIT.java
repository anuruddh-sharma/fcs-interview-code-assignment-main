package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import com.fulfilment.application.monolith.support.EmbeddedPostgresTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
@WithTestResource(EmbeddedPostgresTestResource.class)
public class WarehouseEndpointIT {

  @Test
  public void testSimpleListWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testCreateAndArchiveWarehouse() {
    final String path = "warehouse";
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(
                "{\"businessUnitCode\":\"MWH-IT-1\",\"location\":\"HELMOND-001\",\"capacity\":20,\"stock\":4}")
            .when()
            .post(path)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().get(path + "/" + id).then().statusCode(200).body(containsString("MWH-IT-1"));

    given().when().delete(path + "/" + id).then().statusCode(204);

    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("MWH-IT-1")), containsString("MWH.001"), containsString("AMSTERDAM-001"));
  }
}
