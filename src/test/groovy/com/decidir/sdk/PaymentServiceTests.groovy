package com.decidir.sdk

import com.decidir.sdk.dto.BillingData
import com.decidir.sdk.dto.CardData
import com.decidir.sdk.dto.CardFraudDetectionData
import com.decidir.sdk.dto.CardTokenData
import com.decidir.sdk.dto.Channel
import com.decidir.sdk.dto.Currency
import com.decidir.sdk.dto.CustomerInSite
import com.decidir.sdk.dto.FraudDetectionDataResponse
import com.decidir.sdk.dto.Identification
import com.decidir.sdk.dto.IdentificationType
import com.decidir.sdk.dto.Item
import com.decidir.sdk.dto.PaymentNoPciRequest
import com.decidir.sdk.dto.PaymentPciCardRequest
import com.decidir.sdk.dto.PaymentPciTokenRequest
import com.decidir.sdk.dto.PaymentType
import com.decidir.sdk.dto.PurchaseTotals
import com.decidir.sdk.dto.Status
import com.decidir.sdk.dto.TicketingFraudDetectionData
import com.decidir.sdk.dto.TicketingTransactionData
import com.decidir.sdk.exceptions.PaymentException
import com.decidir.sdk.exceptions.ValidateException
import spock.lang.Specification

/**
 * Created by biandra on 05/10/16.
 */
class PaymentServiceTests extends Specification {

    public static final String secretAccessToken = '00040407'//'4cf891e492384cdeadf211564aa87007'
    public static final String token = "5cebb741-955b-4b82-a7b8-304f94b63594"
    public static final String valid_bin = "450799"
    public static final String user_id = "decidir_test"
    public static final String apiUrl = "http://localhost:9002"


    def decidir
    def billTo
    def purchaseTotals
    def customerInSite
    def ticketingTransactionData

    def setup(){
        decidir = new Decidir(secretAccessToken, apiUrl, 15)
        billTo = new BillingData()
        billTo.city = "Buenos Aires"
        billTo.country = "AR"
        billTo.customer_id = "maxiid"
        billTo.email = "accept@decidir.com.ar"
        billTo.first_name = "Maxi"
        billTo.last_name = "Biandratti"
        billTo.phone_number = "1547766329"
        billTo.postal_code = "1414"
        billTo.state = "BA"
        billTo.street1 = "THAMES 677"
        billTo.ip_address = "190.210.214.252"

        purchaseTotals = new PurchaseTotals()
        purchaseTotals.currency = Currency.ARS
        purchaseTotals.amount = 12444

        customerInSite = new CustomerInSite()
        customerInSite.days_in_site = 243
        customerInSite.is_guest = false
        customerInSite.password = "abracadabra"
        customerInSite.num_of_transactions = 1
        customerInSite.cellphone_number = "12121"

        ticketingTransactionData = new TicketingTransactionData()
        ticketingTransactionData.days_to_event = 55
        ticketingTransactionData.delivery_type = "Pick up"
        def item = new Item()
        item.code = "popblacksabbat2016"
        item.description = "Popular Black Sabbath 2016"
        item.name = "popblacksabbat2016ss"
        item.sku = "asas"
        item.total_amount = 242424
        item.quantity = 2
        item.unit_price = 121212
        ticketingTransactionData.items = Arrays.asList(item)

    }

    def "test payment with CS black error"() {
        setup:
        def fraudDetection = new TicketingFraudDetectionData()
        fraudDetection.purchase_totals = purchaseTotals
        fraudDetection.channel = Channel.WEB
        fraudDetection.customer_in_site = customerInSite
        fraudDetection.device_unique_id = "devicefingerprintid"
        fraudDetection.ticketing_transaction_data = ticketingTransactionData

        def payment = new PaymentNoPciRequest()
        payment.payment_type = PaymentType.SINGLE
        payment.currency = Currency.ARS
        payment.amount = 5
        payment.token = token
        payment.user_id = user_id
        payment.installments = 7
        payment.sub_payments = []
        payment.site_transaction_id = UUID.randomUUID().toString()
        payment.bin = valid_bin
        payment.payment_method_id = 1
        payment.fraud_detection = fraudDetection


        when:
        decidir.payment(payment)

        then:
        def exception = thrown(PaymentException)
        exception.status == 402
        exception.payment.status == Status.REJECTED
        ((FraudDetectionDataResponse)exception.payment.fraud_detection).status.decision == "black"
        exception.payment.fraud_detection.status.reason_code == "-1"
        exception.payment.fraud_detection.status.description == "validation"
        exception.payment.fraud_detection.status.details.error_type == "invalid_request_error"
        exception.payment.fraud_detection.status.details.validation_errors.get(0).code == "missing"
        exception.payment.fraud_detection.status.details.validation_errors.get(0).param == "bill_to"
    }

    def "test confirmPayment valid"() {
        setup:
        def fraudDetection = new TicketingFraudDetectionData()
        fraudDetection.bill_to = billTo
        fraudDetection.purchase_totals = purchaseTotals
        fraudDetection.channel = Channel.WEB
        fraudDetection.customer_in_site = customerInSite
        fraudDetection.device_unique_id = "devicefingerprintid"
        fraudDetection.ticketing_transaction_data = ticketingTransactionData

        def payment = new PaymentNoPciRequest()
        payment.payment_type = PaymentType.SINGLE
        payment.currency = Currency.ARS
        payment.amount = 5
        payment.token = token
        payment.user_id = user_id
        payment.installments = 7
        payment.sub_payments = []
        payment.site_transaction_id = UUID.randomUUID().toString()
        payment.bin = valid_bin
        //payment.site_id=
        payment.payment_method_id = 1
        payment.fraud_detection = fraudDetection


        when:
        def result = decidir.payment(payment)

        then:
        result.status == 201
        result.result.status == Status.APPROVED
        result.result.fraud_detection.status.decision == "green"
        result.result.fraud_detection.status.reason_code == "100"
        result.result.fraud_detection.status.description == "Decision Manager processing"
    }

    def "test confirmPayment PCI with CardData valid"() {
        setup:
        def fraudDetection = new TicketingFraudDetectionData()
        fraudDetection.bill_to = billTo
        fraudDetection.purchase_totals = purchaseTotals
        fraudDetection.channel = Channel.WEB
        fraudDetection.customer_in_site = customerInSite
        fraudDetection.device_unique_id = "devicefingerprintid"
        fraudDetection.ticketing_transaction_data = ticketingTransactionData

        def payment = new PaymentPciCardRequest()
        payment.payment_type = PaymentType.SINGLE
        payment.currency = Currency.ARS
        payment.amount = 5

        payment.user_id = user_id
        payment.installments = 7
        payment.sub_payments = []
        payment.site_transaction_id = UUID.randomUUID().toString()
        payment.bin = valid_bin

        payment.payment_method_id = 1
        payment.fraud_detection = fraudDetection

        def cardData = new CardData()
        cardData.card_number= "4507990000004905"
        cardData.card_expiration_month = "04"
        cardData.card_expiration_year = "20"
        cardData.security_code = "123"
        cardData.card_holder_name ="Juan"

        def cardFraudDetectionData = new CardFraudDetectionData()
        cardFraudDetectionData.device_unique_identifier ="12345"
        cardData.fraud_detection = cardFraudDetectionData

        def cardHolderIdentification = new Identification()
        cardHolderIdentification.number =  "23968498"
        cardHolderIdentification.type = IdentificationType.DNI
        cardData.card_holder_identification = cardHolderIdentification

        payment.card_data = cardData

        when:
        def result = decidir.payment(payment)

        then:
        result.status == 201
        result.result.status == Status.APPROVED
        result.result.fraud_detection.status.decision == "green"
        result.result.fraud_detection.status.reason_code == "100"
        result.result.fraud_detection.status.description == "Decision Manager processing"
    }

    def "test confirmPayment PCI with CardTokenData valid"() {
        setup:
        def fraudDetection = new TicketingFraudDetectionData()
        fraudDetection.bill_to = billTo
        fraudDetection.purchase_totals = purchaseTotals
        fraudDetection.channel = Channel.WEB
        fraudDetection.customer_in_site = customerInSite
        fraudDetection.device_unique_id = "devicefingerprintid"
        fraudDetection.ticketing_transaction_data = ticketingTransactionData

        def payment = new PaymentPciTokenRequest()
        payment.payment_type = PaymentType.SINGLE
        payment.currency = Currency.ARS
        payment.amount = 5

        payment.user_id = user_id
        payment.installments = 7
        payment.sub_payments = []
        payment.site_transaction_id = UUID.randomUUID().toString()
        payment.bin = valid_bin
        payment.payment_method_id = 1
        payment.fraud_detection = fraudDetection

        def cardTokenData = new CardTokenData()
        cardTokenData.token= "c1c7470d-4c22-4656-940a-d159075f52b3"
        cardTokenData.security_code = "123"

        def cardFraudDetectionData = new CardFraudDetectionData()
        cardFraudDetectionData.device_unique_identifier ="12345"
        cardTokenData.fraud_detection = cardFraudDetectionData


        payment.card_token_data = cardTokenData
        when:
        def result = decidir.payment(payment)

        then:
        result.status == 201
        result.result.status == Status.APPROVED
        result.result.fraud_detection.status.decision == "green"
        result.result.fraud_detection.status.reason_code == "100"
        result.result.fraud_detection.status.description == "Decision Manager processing"
    }


    def "test confirmPayment with ValidateException"() {
        setup:
        def fraudDetection = new TicketingFraudDetectionData()
        fraudDetection.bill_to = billTo
        fraudDetection.purchase_totals = purchaseTotals
        fraudDetection.channel = Channel.WEB
        fraudDetection.customer_in_site = customerInSite
        fraudDetection.device_unique_id = "devicefingerprintid"
        fraudDetection.ticketing_transaction_data = ticketingTransactionData

        def payment = new PaymentNoPciRequest()
        payment.payment_type = PaymentType.SINGLE
        payment.currency = Currency.ARS
        payment.amount = 5
        payment.token = token
        payment.user_id = user_id
        payment.installments = 7
        payment.sub_payments = []
        payment.site_transaction_id = UUID.randomUUID().toString()
        payment.bin = "123456"
        payment.payment_method_id = 1
        payment.fraud_detection = fraudDetection

        when:
        decidir.payment(payment)

        then:
        def exception = thrown(ValidateException)
        exception.status == 400
        exception.errorDetail.error_type == "invalid_request_error"
        exception.errorDetail.validation_errors.get(0).code == "Payment"
        exception.errorDetail.validation_errors.get(0).param == "bin"
        exception.message == "Bad Request"
    }

    def "test list of payments"() {
        when:
        def decidirResponse = decidir.getPayments(null, null, null, null)

        then:
        decidirResponse.status == 200
        decidirResponse.result != null
        decidirResponse.message == "OK"
    }
    
    def "test get payment"() {
        when:
        def payments = decidir.getPayments(null, null, null, null)
        def payment = decidir.getPayment(payments.result.results[0].id)

        then:
        payment != null
        payment.result.amount == payments.result.results[0].amount
    }
}
