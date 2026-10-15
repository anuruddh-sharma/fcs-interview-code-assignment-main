package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fulfilment.application.monolith.support.EmbeddedPostgresTestResource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
@WithTestResource(EmbeddedPostgresTestResource.class)
public class StoreEndpointTest {

  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Inject TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  @Test
  public void listsStores() {
    given()
        .when()
        .get("/store")
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"));
  }

  @Test
  public void missingStoreReturns404() {
    given().when().get("/store/99999").then().statusCode(404);
  }

  @Test
  public void createUpdateAndPatchNotifyLegacyOnlyAfterCommit() {
    String name = "STORE" + UUID.randomUUID().toString().substring(0, 8);
    AtomicInteger statusDuringCreate = new AtomicInteger(-1);
    AtomicInteger statusDuringUpdate = new AtomicInteger(-1);
    doAnswer(
            invocation -> {
              statusDuringCreate.set(transactionSynchronizationRegistry.getTransactionStatus());
              return null;
            })
        .when(legacyStoreManagerGateway)
        .createStoreOnLegacySystem(any());
    doAnswer(
            invocation -> {
              statusDuringUpdate.set(transactionSynchronizationRegistry.getTransactionStatus());
              return null;
            })
        .when(legacyStoreManagerGateway)
        .updateStoreOnLegacySystem(any());

    Number id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":4}")
            .when()
            .post("/store")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    ArgumentCaptor<Store> created = ArgumentCaptor.forClass(Store.class);
    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(created.capture());
    assertEquals(name, created.getValue().name);
    assertEquals(4, created.getValue().quantityProductsInStock);
    assertNotNull(created.getValue().id);
    assertNotEquals(Status.STATUS_ACTIVE, statusDuringCreate.get());

    String renamed = name + "B";
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + renamed + "\",\"quantityProductsInStock\":8}")
        .when()
        .put("/store/" + id.longValue())
        .then()
        .statusCode(200)
        .body(containsString(renamed));

    ArgumentCaptor<Store> updated = ArgumentCaptor.forClass(Store.class);
    verify(legacyStoreManagerGateway, times(1)).updateStoreOnLegacySystem(updated.capture());
    assertEquals(id.longValue(), updated.getValue().id);
    assertEquals(renamed, updated.getValue().name);
    assertEquals(8, updated.getValue().quantityProductsInStock);
    assertNotEquals(Status.STATUS_ACTIVE, statusDuringUpdate.get());

    String patched = name + "C";
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + patched + "\",\"quantityProductsInStock\":2}")
        .when()
        .patch("/store/" + id.longValue())
        .then()
        .statusCode(200);

    verify(legacyStoreManagerGateway, times(2)).updateStoreOnLegacySystem(any());
    assertNotEquals(Status.STATUS_ACTIVE, statusDuringUpdate.get());
  }

  @Test
  public void invalidUpdateDoesNotNotifyLegacy() {
    given()
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .put("/store/1")
        .then()
        .statusCode(422);

    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any());
  }

  @Test
  public void rolledBackCreateDoesNotNotifyLegacy() {
    String name = "DUP" + UUID.randomUUID().toString().substring(0, 8);
    String body = "{\"name\":\"" + name + "\",\"quantityProductsInStock\":1}";

    given().contentType(ContentType.JSON).body(body).when().post("/store").then().statusCode(201);
    given().contentType(ContentType.JSON).body(body).when().post("/store").then().statusCode(500);

    verify(legacyStoreManagerGateway, times(1)).createStoreOnLegacySystem(any());
  }

  @Test
  public void rejectsCreateWhenIdIsSet() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":10,\"name\":\"HASID123\",\"quantityProductsInStock\":1}")
        .when()
        .post("/store")
        .then()
        .statusCode(422);

    verify(legacyStoreManagerGateway, never()).createStoreOnLegacySystem(any());
  }
}
