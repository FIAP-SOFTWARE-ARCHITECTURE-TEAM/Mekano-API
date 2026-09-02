package com.fiap.mekano.rest.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "atendente-test", roles = { "atendente" })
public class VeiculoResourceTest {

        @InjectMock
        ClienteRepositoryPort clienteRepository;

        private static final UUID CLIENTE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

        @BeforeEach
        void setup() {

                Cliente fakeCliente = Cliente.reconstitute(
                                CLIENTE_UUID,
                                "Cliente Teste",
                                "52998224725",
                                "cliente@teste.com",
                                null,
                                "Rua A", "100", "Centro", "São Paulo", "SP", "01001000",
                                LocalDateTime.now());

                when(clienteRepository.findById(CLIENTE_UUID))
                                .thenReturn(Optional.of(fakeCliente));
        }

        /** CENÁRIO 1 - Criação com sucesso */
        @Test
        @Order(1)
        void create_validVehicle_returns201() {

                UUID clienteUuid = CLIENTE_UUID;

                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "clienteUuid":"%s",
                                                  "placa":"ABC1234",
                                                  "marca":"Toyota",
                                                  "modelo":"Corolla",
                                                  "ano":2020
                                                }
                                                """.formatted(clienteUuid))
                                .when()
                                .post("/api/v1/veiculos")
                                .then()
                                .statusCode(201)
                                .header("Location", notNullValue())
                                .body("id", notNullValue())
                                .body("clienteUuid", equalTo(clienteUuid.toString()))
                                .body("placa", equalTo("ABC1234"))
                                .body("marca", equalTo("Toyota"))
                                .body("modelo", equalTo("Corolla"))
                                .body("ano", equalTo(2020));
        }

        /** CENÁRIO 2 - Placa duplicada */
        @Test
        @Order(2)
        void create_duplicatePlate_returns409() {

                UUID clienteUuid = CLIENTE_UUID;

                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "clienteUuid":"%s",
                                                  "placa":"ABC1234",
                                                  "marca":"Honda",
                                                  "modelo":"Civic",
                                                  "ano":2021
                                                }
                                                """.formatted(clienteUuid))
                                .when()
                                .post("/api/v1/veiculos")
                                .then()
                                .statusCode(409);
        }

        /** CENÁRIO 3 - Placa Inválida */
        @Test
        void create_invalidPlate_returns400() {

                UUID clienteUuid = CLIENTE_UUID;

                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "clienteUuid":"%s",
                                                  "placa":"INVALIDA",
                                                  "marca":"Toyota",
                                                  "modelo":"Corolla",
                                                  "ano":2020
                                                }
                                                """.formatted(clienteUuid))
                                .when()
                                .post("/api/v1/veiculos")
                                .then()
                                .statusCode(400)
                                .body("violations", notNullValue());
        }

        /** CENÁRIO 4 - Normalização da placa */
        @Test
        void create_plateWithHyphen_shouldNormalize() {

                UUID clienteUuid = CLIENTE_UUID;

                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "clienteUuid":"%s",
                                                  "placa":"DEF-5678",
                                                  "marca":"Toyota",
                                                  "modelo":"Corolla",
                                                  "ano":2020
                                                }
                                                """.formatted(clienteUuid))
                                .when()
                                .post("/api/v1/veiculos")
                                .then()
                                .statusCode(201)
                                .body("placa", equalTo("DEF5678"));
        }

        /** CENÁRIO 5 - Update de veículo */
        @Test
        void update_vehicle_returns200() {

                String location = given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "clienteUuid":"11111111-1111-1111-1111-111111111111",
                                                  "placa":"XYZ1234",
                                                  "marca":"Toyota",
                                                  "modelo":"Corolla",
                                                  "ano":2020
                                                }
                                                """)
                                .when()
                                .post("/api/v1/veiculos")
                                .then()
                                .statusCode(201)
                                .extract()
                                .header("Location");

                UUID id = UUID.fromString(
                                location.substring(location.lastIndexOf("/") + 1));

                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "placa":"ABC1D23",
                                                  "marca":"Toyota",
                                                  "modelo":"Yaris",
                                                  "ano":2022
                                                }
                                                """)
                                .when()
                                .put("/api/v1/veiculos/{id}", id)
                                .then()
                                .statusCode(200)
                                .body("placa", equalTo("XYZ1234"))
                                .body("marca", equalTo("Toyota"))
                                .body("modelo", equalTo("Yaris"))
                                .body("ano", equalTo(2022));
        }

        /** CENÁRIO 6 - Soft delete do veículo */
        @Test
        void delete_vehicle_returns204() {

                String location = given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "clienteUuid":"11111111-1111-1111-1111-111111111111",
                                                  "placa":"JKL1234",
                                                  "marca":"Honda",
                                                  "modelo":"Civic",
                                                  "ano":2021
                                                }
                                                """)
                                .when()
                                .post("/api/v1/veiculos")
                                .then()
                                .statusCode(201)
                                .extract()
                                .header("Location");

                UUID id = UUID.fromString(
                                location.substring(location.lastIndexOf("/") + 1));

                given()
                                .when()
                                .delete("/api/v1/veiculos/{id}", id)
                                .then()
                                .statusCode(204);

                given()
                                .when()
                                .get("/api/v1/veiculos/{id}", id)
                                .then()
                                .statusCode(200)
                                .body("isActive", equalTo(false));
        }

        /** CENÁRIO 6b - Reativação de veículo inativo */
        @Test
        void reativar_vehicle_returns204AndActive() {

                String location = given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "clienteUuid":"11111111-1111-1111-1111-111111111111",
                                                  "placa":"KLM1234",
                                                  "marca":"Fiat",
                                                  "modelo":"Pulse",
                                                  "ano":2023
                                                }
                                                """)
                                .when()
                                .post("/api/v1/veiculos")
                                .then()
                                .statusCode(201)
                                .extract()
                                .header("Location");

                UUID id = UUID.fromString(
                                location.substring(location.lastIndexOf("/") + 1));

                given()
                                .when()
                                .delete("/api/v1/veiculos/{id}", id)
                                .then()
                                .statusCode(204);

                given()
                                .when()
                                .put("/api/v1/veiculos/{id}/ativar", id)
                                .then()
                                .statusCode(204);

                given()
                                .when()
                                .get("/api/v1/veiculos/{id}", id)
                                .then()
                                .statusCode(200)
                                .body("isActive", equalTo(true));
        }

        /** CENÁRIO 6c - Reativação de veículo já ativo é idempotente (204) */
        @Test
        void reativar_activeVehicle_returns204() {

                String location = given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "clienteUuid":"11111111-1111-1111-1111-111111111111",
                                                  "placa":"MNO1234",
                                                  "marca":"Chevrolet",
                                                  "modelo":"Onix",
                                                  "ano":2022
                                                }
                                                """)
                                .when()
                                .post("/api/v1/veiculos")
                                .then()
                                .statusCode(201)
                                .extract()
                                .header("Location");

                UUID id = UUID.fromString(
                                location.substring(location.lastIndexOf("/") + 1));

                given()
                                .when()
                                .put("/api/v1/veiculos/{id}/ativar", id)
                                .then()
                                .statusCode(204);

                given()
                                .when()
                                .get("/api/v1/veiculos/{id}", id)
                                .then()
                                .statusCode(200)
                                .body("isActive", equalTo(true));
        }

        /** CENÁRIO 6d - Update de veículo inativo (PUT não deve tratar inativo como inexistente) */
        @Test
        void update_inactiveVehicle_returns200AndPreservesInactive() {

                String location = given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "clienteUuid":"11111111-1111-1111-1111-111111111111",
                                                  "placa":"OPQ1234",
                                                  "marca":"Renault",
                                                  "modelo":"Kwid",
                                                  "ano":2021
                                                }
                                                """)
                                .when()
                                .post("/api/v1/veiculos")
                                .then()
                                .statusCode(201)
                                .extract()
                                .header("Location");

                UUID id = UUID.fromString(
                                location.substring(location.lastIndexOf("/") + 1));

                given()
                                .when()
                                .delete("/api/v1/veiculos/{id}", id)
                                .then()
                                .statusCode(204);

                given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "marca":"Renault",
                                                  "modelo":"Kwid Outsider",
                                                  "ano":2022
                                                }
                                                """)
                                .when()
                                .put("/api/v1/veiculos/{id}", id)
                                .then()
                                .statusCode(200)
                                .body("modelo", equalTo("Kwid Outsider"))
                                .body("ano", equalTo(2022))
                                .body("isActive", equalTo(false));

                given()
                                .when()
                                .get("/api/v1/veiculos/{id}", id)
                                .then()
                                .statusCode(200)
                                .body("isActive", equalTo(false));
        }

        /** CENÁRIO 6e - Listagem com filtro isActive */
        @Test
        void list_filtroIsActive_returnsOnlyRequested() {

                String location = given()
                                .contentType(ContentType.JSON)
                                .body("""
                                                {
                                                  "clienteUuid":"11111111-1111-1111-1111-111111111111",
                                                  "placa":"FIL1234",
                                                  "marca":"VW",
                                                  "modelo":"Gol",
                                                  "ano":2019
                                                }
                                                """)
                                .when()
                                .post("/api/v1/veiculos")
                                .then()
                                .statusCode(201)
                                .extract()
                                .header("Location");

                UUID id = UUID.fromString(
                                location.substring(location.lastIndexOf("/") + 1));

                given()
                                .when()
                                .delete("/api/v1/veiculos/{id}", id)
                                .then()
                                .statusCode(204);

                given()
                                .when()
                                .get("/api/v1/veiculos?isActive=false&size=100")
                                .then()
                                .statusCode(200)
                                .body("content.placa", hasItem("FIL1234"));

                given()
                                .when()
                                .get("/api/v1/veiculos?isActive=true&size=100")
                                .then()
                                .statusCode(200)
                                .body("content.placa", not(hasItem("FIL1234")));

                given()
                                .when()
                                .get("/api/v1/veiculos?size=100")
                                .then()
                                .statusCode(200)
                                .body("content.placa", hasItem("FIL1234"));
        }

        /** CENÁRIO 7 - Autorização */
        @Test
        @TestSecurity(user = "mecanico-test", roles = { "mecanico" })
        void mecanico_cannot_access_vehicleEndpoints() {

                given()
                                .when()
                                .get("/api/v1/veiculos")
                                .then()
                                .statusCode(403);
        }

}
