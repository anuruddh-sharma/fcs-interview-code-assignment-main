package com.fulfilment.application.monolith.fulfilment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import com.fulfilment.application.monolith.support.EmbeddedPostgresTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(EmbeddedPostgresTestResource.class)
public class FulfilmentEndpointTest {

  @Test
  public void associatesProductsWithinTheThreeLimits() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    long storeId = createStore("FUL" + suffix);
    long firstWarehouse = createWarehouse("MWH-FUL-A-" + suffix, "AMSTERDAM-001", 10, 1);
    long secondWarehouse = createWarehouse("MWH-FUL-B-" + suffix, "AMSTERDAM-002", 10, 1);
    long thirdWarehouse = createWarehouse("MWH-FUL-C-" + suffix, "EINDHOVEN-001", 10, 1);
    long fourthWarehouse = createWarehouse("MWH-FUL-D-" + suffix, "EINDHOVEN-001", 10, 1);

    long product = createProduct("P-LIMIT-" + suffix);
    long firstAssociation = associate(storeId, product, firstWarehouse);
    associate(storeId, product, secondWarehouse);

    given()
        .contentType(ContentType.JSON)
        .body(assignment(storeId, product, thirdWarehouse))
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(400)
        .body(containsString("at most 2 different warehouses"));

    long storeForWarehouseLimit = createStore("FULW" + suffix);
    associate(storeForWarehouseLimit, createProduct("PA" + suffix), firstWarehouse);
    associate(storeForWarehouseLimit, createProduct("PB" + suffix), secondWarehouse);
    associate(storeForWarehouseLimit, createProduct("PC" + suffix), thirdWarehouse);
    given()
        .contentType(ContentType.JSON)
        .body(assignment(storeForWarehouseLimit, createProduct("PD" + suffix), fourthWarehouse))
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(400)
        .body(containsString("at most 3 different warehouses"));

    long productWarehouse = createWarehouse("MWH-FUL-P-" + suffix, "AMSTERDAM-001", 10, 1);
    long productStore = createStore("FULP" + suffix);
    for (int i = 0; i < 5; i++) {
      associate(productStore, createProduct("PW" + i + suffix), productWarehouse);
    }
    given()
        .contentType(ContentType.JSON)
        .body(assignment(productStore, createProduct("PW6" + suffix), productWarehouse))
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(400)
        .body(containsString("at most 5 types of products"));

    given()
        .contentType(ContentType.JSON)
        .body(assignment(storeId, product, firstWarehouse))
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(409);

    given()
        .when()
        .get("/fulfilment?storeId=" + storeId)
        .then()
        .statusCode(200)
        .body("$", hasSize(2));

    given().when().delete("/fulfilment/" + firstAssociation).then().statusCode(204);
    given().when().delete("/fulfilment/" + firstAssociation).then().statusCode(404);
    given().when().delete("/fulfilment/99999").then().statusCode(404);

    given()
        .contentType(ContentType.JSON)
        .body(assignment(99999L, product, firstWarehouse))
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(404)
        .body(containsString("Store not found"));

    given()
        .contentType(ContentType.JSON)
        .body(assignment(storeId, 99999L, firstWarehouse))
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(404)
        .body(containsString("Product not found"));

    long archivedWarehouse = createWarehouse("MWH-FUL-X-" + suffix, "HELMOND-001", 10, 1);
    given().when().delete("/warehouse/" + archivedWarehouse).then().statusCode(204);
    given()
        .contentType(ContentType.JSON)
        .body(assignment(storeId, createProduct("ARCH" + suffix), archivedWarehouse))
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(404)
        .body(containsString("Warehouse not found"));

    given()
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(400)
        .body(containsString("are required"));
  }

  private static long createProduct(String name) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + name + "\",\"stock\":1}")
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static long createStore(String name) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":1}")
        .when()
        .post("/store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static long createWarehouse(String code, String location, int capacity, int stock) {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(
                "{\"businessUnitCode\":\""
                    + code
                    + "\",\"location\":\""
                    + location
                    + "\",\"capacity\":"
                    + capacity
                    + ",\"stock\":"
                    + stock
                    + "}")
            .when()
            .post("/warehouse")
            .then()
            .statusCode(201)
            .extract()
            .path("id");
    return Long.parseLong(id);
  }

  private static long associate(long storeId, long productId, long warehouseId) {
    return given()
        .contentType(ContentType.JSON)
        .body(assignment(storeId, productId, warehouseId))
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static String assignment(long storeId, long productId, long warehouseId) {
    return "{\"storeId\":" + storeId + ",\"productId\":" + productId + ",\"warehouseId\":" + warehouseId + "}";
  }
}
