package com.rbkmoney;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.proxy_inspector.InvoicePayment;
import com.rbkmoney.damsel.proxy_inspector.Party;
import com.rbkmoney.damsel.proxy_inspector.Shop;
import com.rbkmoney.damsel.proxy_inspector.*;
import com.rbkmoney.woody.thrift.impl.http.THClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
            Assert.assertEquals(RiskScore.high, riskScore);
        }
        Thread.sleep(1000L);
        RiskScore riskScore = inspectPayment.inspectPayment(createContext(LocalDateTime.now().atZone(ZoneOffset.UTC).toString()));
        Assert.assertEquals(RiskScore.fatal, riskScore);
    }

    private Context createContext(String createdAt) {
        return new Context(
                new PaymentInfo(
                        new Shop("shopTest",
                                new Category("pizza", "no category"),
                                new ShopDetails("pizza-sushi"),
                                new ShopLocation() {{
                                    setUrl("http://www.pizza-sushi.com/");
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
                                                        .setPaymentSystem(BankCardPaymentSystem.visa)
                                                        .setBin("427640")
                                                        .setLastDigits("6395")
                                                        .setIssuerCountry(Residence.RUS))))),
                                new Cash()
                                        .setAmount(100L)
                                        .setCurrency(new CurrencyRef()
                                                .setSymbolicCode("RUB"))),
                        new com.rbkmoney.damsel.proxy_inspector.Invoice(
                                "partyTest",
                                createdAt,
                                "",
                                new InvoiceDetails("drugs guns murder")),
                        new Party("partyTest")
                )
        );
    }

}