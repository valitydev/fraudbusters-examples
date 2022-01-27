package com.rbkmoney;

import dev.vality.damsel.domain.*;
import dev.vality.damsel.proxy_inspector.InvoicePayment;
import dev.vality.damsel.proxy_inspector.Party;
import dev.vality.damsel.proxy_inspector.Shop;
import dev.vality.damsel.proxy_inspector.*;
import dev.vality.woody.thrift.impl.http.THClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class SimpleFraudbustersInvocationTest {

    InspectorProxySrv.Iface inspectPayment = new THClientBuilder()
            .withAddress(initUrlAddress())
            .withNetworkTimeout(300000)
            .build(InspectorProxySrv.Iface.class);

    private URI initUrlAddress() {
        try {
            return new URI(String.format("http://0.0.0.0:%s/fraud_inspector/v1", 8999));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void callAntifraud() throws TException, InterruptedException {
        for (int i = 0; i < 10; i++) {
            RiskScore riskScore = inspectPayment.inspectPayment(createContext(LocalDateTime.now().atZone(ZoneOffset.UTC).toString()));
            assertEquals(RiskScore.high, riskScore);
        }
        Thread.sleep(1000L);
        RiskScore riskScore = inspectPayment.inspectPayment(createContext(LocalDateTime.now().atZone(ZoneOffset.UTC).toString()));
        assertEquals(RiskScore.fatal, riskScore);
    }

    private Context createContext(String createdAt) {
        return new Context(
                new PaymentInfo(
                        new Shop("shopTest",
                                new Category("pizza", "no category"),
                                new ShopDetails("onlystruga"),
                                new ShopLocation() {{
                                    setUrl("http://www.onlystruga.com/");
                                }}
                        ),
                        new InvoicePayment("payment_1",
                                createdAt,
                                Payer.payment_resource(new PaymentResourcePayer()
                                        .setContactInfo(new ContactInfo()
                                                .setEmail("test@mail.ru"))
                                        .setResource(new DisposablePaymentResource()
                                                .setClientInfo(new ClientInfo()
                                                        .setIpAddress("123.123.123.123")
                                                        .setFingerprint("xxxxx"))
                                                .setPaymentTool(PaymentTool.bank_card(new BankCard()
                                                        .setToken("4J8vmnlYPwzYzia74fny81")
                                                        .setPaymentSystem(new PaymentSystemRef().setId("visa"))
                                                        .setBin("427640")
                                                        .setLastDigits("6395")
                                                        .setIssuerCountry(CountryCode.RUS))))),
                                new Cash()
                                        .setAmount(100L)
                                        .setCurrency(new CurrencyRef()
                                                .setSymbolicCode("RUB"))),
                        new dev.vality.damsel.proxy_inspector.Invoice(
                                "partyTest",
                                createdAt,
                                "",
                                new InvoiceDetails("drugs guns murder")),
                        new Party("partyTest")
                )
        );
    }

}