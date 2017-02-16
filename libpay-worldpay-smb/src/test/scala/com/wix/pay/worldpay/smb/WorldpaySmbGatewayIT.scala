package com.wix.pay.worldpay.smb

import java.util.Locale

import com.wix.pay.creditcard._
import com.wix.pay.model.{CurrencyAmount, Deal, Payment, ShippingAddress}
import com.wix.pay.worldpay.smb.parsers.{JsonWorldpaySmbAuthorizationParser, JsonWorldpaySmbMerchantParser}
import com.wix.pay.worldpay.smb.testkit.WorldpaySmbDriver
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope
import spray.http.StatusCodes

class WorldpaySmbGatewayIT extends SpecWithJUnit with WorldpayMatcherSupport {
  val probePort = 10001
  val driver = new WorldpaySmbDriver(probePort)

  val someOrderCode = "someOrderCode"
  val serviceKey = "someServiceKey"
  val someMerchant = JsonWorldpaySmbMerchantParser.stringify(WorldpaySmbMerchant(serviceKey))
  val someAuthorization = JsonWorldpaySmbAuthorizationParser.stringify(WorldpaySmbAuthorization(someOrderCode))

  step {
    driver.start()
  }

  sequential

  "authorize request" should {
    "successfully yield an authorization key upon a valid request" in new Ctx {
      givenWorldpayAuthorizationRequest returns someOrderCode
      authorize() must beSuccessfulTry.withValue(someAuthorization)
    }

    "fail with PaymentRejectedException for rejected transactions" in new Ctx {
      givenWorldpayAuthorizationRequest isRejectedWith(someOrderCode, "Some error message")
      authorize() must beRejectedWithMessage("Some error message")
    }

    "fail with PaymentRejectedException for 'Bad Request 400' response" in new Ctx {
      givenWorldpayAuthorizationRequest isAnErrorWith(StatusCodes.BadRequest, "Some error message")
      authorize() must beRejectedWithMessage("Some error message")
    }

    "fail with PaymentErrorException for erroneous response" in new Ctx {
      givenWorldpayAuthorizationRequest isAnErrorWith(StatusCodes.Unauthorized, "Something bad happened")
      authorize() must failWithMessage("Something bad happened")
    }
  }

  "capture request" should {
    "successfully yield an orderCode upon a valid request" in new Ctx {
      givenWorldpayCaptureRequest returns someOrderCode
      capture() must beSuccessfulTry.withValue(someOrderCode)
    }

    "fail with PaymentRejectedException for 'Bad Request 400' response" in new Ctx {
      givenWorldpayCaptureRequest isAnErrorWith(StatusCodes.BadRequest, "Some error message")
      capture() must beRejectedWithMessage("Some error message")
    }

    "fail with PaymentErrorException for erroneous response" in new Ctx {
      givenWorldpayCaptureRequest isAnErrorWith(StatusCodes.Unauthorized, "Something bad happened")
      capture() must failWithMessage("Something bad happened")
    }
  }

  "sale request" should {
    "successfully yield an authorization key upon a valid request" in new Ctx {
      givenWorldpaySaleRequest returns someOrderCode
      sale() must beSuccessfulTry.withValue(someOrderCode)
    }

    "fail with PaymentRejectedException for rejected transactions" in new Ctx {
      givenWorldpaySaleRequest isRejectedWith(someOrderCode, "Some error message")
      sale() must beRejectedWithMessage("Some error message")
    }

    "fail with PaymentRejectedException for 'Bad Request 400' response" in new Ctx {
      givenWorldpaySaleRequest isAnErrorWith(StatusCodes.BadRequest, "Some error message")
      sale() must beRejectedWithMessage("Some error message")
    }

    "fail with PaymentErrorException for erroneous response" in new Ctx {
      givenWorldpaySaleRequest isAnErrorWith(StatusCodes.Unauthorized, "Something bad happened")
      sale() must failWithMessage("Something bad happened")
    }
  }

  "voidAuthorization request" should {
    "successfully yield an authorization key upon a valid request" in new Ctx {
      givenWorldpayVoidAuthorizationRequest returns someOrderCode
      voidAuthorization() must beSuccessfulTry.withValue(someOrderCode)
    }

    "fail with PaymentRejectedException for 'Bad Request 400' response" in new Ctx {
      givenWorldpayVoidAuthorizationRequest isAnErrorWith(StatusCodes.BadRequest, "Some error message")
      voidAuthorization() must beRejectedWithMessage("Some error message")
    }

    "fail with PaymentErrorException for erroneous response" in new Ctx {
      givenWorldpayVoidAuthorizationRequest isAnErrorWith(StatusCodes.Unauthorized, "Something bad happened")
      voidAuthorization() must failWithMessage("Something bad happened")
    }
  }

  step {
    driver.stop()
  }

  trait Ctx extends Scope {
    val worldpayGateway = new WorldpaySmbGateway(s"http://localhost:$probePort")

    driver.reset()

    val creditCard = CreditCard("4580458045804580", YearMonth(2020, 12), Some(CreditCardOptionalFields(
      csc = Some("123"),
      publicFields = Some(PublicCreditCardOptionalFields(
        holderId = None,
        holderName = Some("Some Name"),
        billingAddressDetailed = Some(AddressDetailed(
          street = Some("billingStreet"),
          city = Some("billingCity"),
          state = Some("billingState"),
          postalCode = Some("billingPostalCode"),
          countryCode = Some(Locale.GERMANY)
        ))
      ))
    )))

    val deal = Deal(
      id = "123",
      title = Some("title"),
      description = Some("desc"),
      invoiceId = Some("invoiceId"),
      shippingAddress = Some(ShippingAddress(
        firstName = Some("firstName"),
        lastName = Some("lastName"),
        address = Some(AddressDetailed(
          street = Some("shippingStreet"),
          city = Some("shippingCity"),
          postalCode = Some("shippingPostalCode"),
          state = Some("shippingState"),
          countryCode = Some(Locale.CHINA)
        ))
      )))

    val currencyAmount = CurrencyAmount("USD", 5.67)
    val payment = Payment(currencyAmount, installments = 1)

    def givenWorldpayAuthorizationRequest = driver.anAuthorizationRequest(serviceKey, creditCard, currencyAmount, Some(deal))
    def authorize() = worldpayGateway.authorize(someMerchant, creditCard, payment, None, Some(deal))

    def givenWorldpayCaptureRequest = driver.aCaptureRequest(serviceKey, someOrderCode, creditCard, currencyAmount, Some(deal))
    def capture() = worldpayGateway.capture(someMerchant, someAuthorization, currencyAmount.amount)

    def givenWorldpaySaleRequest = driver.aSaleRequest(serviceKey, creditCard, currencyAmount, Some(deal))
    def sale() = worldpayGateway.sale(someMerchant, creditCard, payment, None, Some(deal))

    def givenWorldpayVoidAuthorizationRequest = driver.aVoidAuthorizationRequest(serviceKey, someOrderCode)
    def voidAuthorization() = worldpayGateway.voidAuthorization(someMerchant, someAuthorization)
  }
}