package com.example.paymentsdk;
/*
Here we store the configuration setup for the Braintree SDK
Like the required credentials and also REQUEST_CODE for intent
Result.
*/
public class Config {
    //Braintree Merchant ID
    public static final String MERCHANT_ID        = "8mrh2hk3n57nfqff";
    //Braintree Public key
    public static final String PUBLIC_KEY         = "9dh48bqntry36kpf";
    //Braintree Private key
    public static final String PRIVATE_KEY        = "731ea29addff02e31e2d4431c9e09522";
    //Braintree Provided Nonce for sandbox testing
    public static final String NONCE              = "fake-valid-nonce";
    //Tokenization key
    public static String TOKENZATION_KEY          = "sandbox_x6sbt3jm_8mrh2hk3n57nfqff";
    //Request code
    public static final int REQUEST_CODE          = 11;
}
