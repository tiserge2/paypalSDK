package com.example.paymentsdk;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;
import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.Authorization;

import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity {

    Button paypalButton;
    BraintreeFragment mBraintreeFragment;
    Authorization mAuthorization;
    final BraintreeGateway gateway = new BraintreeGateway(
            Environment.SANDBOX,
            "8mrh2hk3n57nfqff",
            "9dh48bqntry36kpf",
            "731ea29addff02e31e2d4431c9e09522"
    );

    String token = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(new Runnable() {
            @Override
            public void run() {
                token = gateway.clientToken().generate();
                paypalButton = findViewById(R.id.paypalButton);
                paypalButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        System.out.println("Clickkkk");
                        DropInRequest dropInRequest = new DropInRequest()
                                .tokenizationKey("sandbox_x6sbt3jm_8mrh2hk3n57nfqff");
                        startActivityForResult(dropInRequest.getIntent(getApplicationContext()), 11);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 11) {
            if (resultCode == RESULT_OK) {
                final DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                // use the result to update your UI and send the payment method nonce to your server
                System.out.println("==============>>> user success.");
                System.out.println("Showing the nonce: " + result.toString());


                new Thread(new Runnable() {
                    public void run() {
                        TransactionRequest request = new TransactionRequest()
                                .amount(new BigDecimal("1000.00"))
                                .paymentMethodNonce("fake-valid-nonce")
                                .options()
                                    .done();
                        Result<Transaction> saleResult = gateway.transaction().sale(request);
                        if (saleResult.isSuccess()) {
                            Transaction transaction = saleResult.getTarget();
                            System.out.println("Success ID: " + transaction.getId());
                        } else {
                            System.out.println("Message: " + saleResult.getMessage());
                        }
                    }
                }).start();
            } else if (resultCode == RESULT_CANCELED) {
                // the user canceled
                System.out.println("==============>>> user canceled.");
            } else {
                // handle errors here, an exception may be available in
                Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                System.out.println(error);
                System.out.println("==============>>> error.");
            }
        }
    }
}