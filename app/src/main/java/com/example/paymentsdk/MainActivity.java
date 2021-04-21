package com.example.paymentsdk;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;

import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity {

    Button paypalButton;
    EditText editAmount;
    //braintree gateway
    BraintreeGateway gateway = null;
    //Tokenization receiver
    String token = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        paypalButton    = findViewById(R.id.paypalButton);
        editAmount      = findViewById(R.id.editAmount);
        //setup braintree gateway
        initializeGateway();
    }

    /*
     * When the user click on the checkout button handleCheckout
     * Will be responsible to take the required action based on
     * certain conditon. First we check if the amount text input
     * is not empty to proceed with the Braintree Drop-In UI
     * */
    public void handleCheckout(View v) {
        if (TextUtils.isEmpty(editAmount.getText().toString())) {
            Toast.makeText(getApplicationContext(), "Amount cannot be empty.", Toast.LENGTH_SHORT).show();
        } else {
            //Proceed with Drop In UI
            setupDropInUI();
            //Disable button with loading spinner
            paypalButton.setEnabled(false);
            paypalButton.setText(R.string.processing);
        }
    }

    /*
     * We need to create a DropInRequest in order to have the Paypal payment
     * Option display with the DropInUI
     * */
    public void setupDropInUI() {
        // we need to use a thread to avoid any blocking issue while generating the token
        new Thread(new Runnable() {
            @Override
            public void run() {
                //We generate the client token in order to proceed with the Braintree Drop-In UI
                token = gateway.clientToken().generate();
                DropInRequest dropInRequest = new DropInRequest()
                        .clientToken(token);
                startActivityForResult(dropInRequest.getIntent(getApplicationContext()), Config.REQUEST_CODE);
            }
        }).start();
    }

    /*
     * configure the braintree gateway with the credentials
     * */
    public void initializeGateway() {
        gateway = new BraintreeGateway(
                Environment.SANDBOX,
                Config.MERCHANT_ID,
                Config.PUBLIC_KEY,
                Config.PRIVATE_KEY
        );
    }

    /*
     * Once the client has entered information about his paypal, a Nonce is sent to the main activity
     * With the REQUEST_CODE if everything was good, otherwise we shall have other code
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Config.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                //if everything relating to the user's paypal credential is fine we're going to setup the transaction
                setupTransaction();
            } else if (resultCode == RESULT_CANCELED) {
                //enable the paypal button again
                paypalButton.setEnabled(true);
                paypalButton.setText(R.string.Checkout);
                //if the user cancelled the paypal credential gathering process half way, we bring a short messag
                Toast.makeText(getApplicationContext(), "Message: " + "Transaction has been cancelled.", Toast.LENGTH_SHORT).show();
            } else {
                // handle errors here, an exception may be available in
                assert data != null;
                Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                System.out.println(error.getMessage());
                //enable the paypal button again
                paypalButton.setEnabled(true);
                paypalButton.setText(R.string.checkout);
                Toast.makeText(getApplicationContext(), "Message: " + "System error, please try again later.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
     * Here we configure the Transaction with the fake nonce as we are testing
     * We get the amount from user input and add it to the request.
     * This process should have been done from server side,
     * So using a thread keep us from having issue and also mimic
     * The server side for us
     * */
    public void setupTransaction() {
        //get the amount entered by the user
        final String amount = editAmount.getText().toString();
        //ask the user if he still want to process the transaction after setting up paypal account
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Confirm transaction.")
                .setMessage("Transaction amount: " + amount + " USD")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            public void run() {
                                //prepare the transaction to be sent to braintree server
                                TransactionRequest request = new TransactionRequest()
                                        .amount(new BigDecimal(amount))
                                        .paymentMethodNonce(Config.NONCE)
                                        .options()
                                        .submitForSettlement(true)
                                        .done();
                                final Result<Transaction> saleResult = gateway.transaction().sale(request);
                                if (saleResult.isSuccess()) {
                                    // if the transaction has been setup we show an alert information dialog to the user
                                    final Transaction transaction = saleResult.getTarget();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            editAmount.setText("");
                                            //enable the paypal button again
                                            paypalButton.setEnabled(true);
                                            paypalButton.setText(R.string.Checkout);
                                            new AlertDialog.Builder(MainActivity.this)
                                                    .setTitle("Payment Successful")
                                                    .setMessage("Transaction ID: " + transaction.getId() +
                                                            "\nAmount: " + transaction.getAmount() + " USD" +
                                                            "\nDate: " + transaction.getCreatedAt().getTime() +
                                                            "\nStatus: " + transaction.getStatus())
                                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    })
                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                                    .show();
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // if any other error while processing the payment we show a message to user
                                            new AlertDialog.Builder(MainActivity.this)
                                                    .setTitle("Transaction error")
                                                    .setMessage("Message: " + saleResult.getMessage())
                                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                            //enable the paypal button again
                                                            paypalButton.setEnabled(true);
                                                            paypalButton.setText(R.string.checkout);
                                                        }
                                                    })
                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                                    .show();
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        //enable the checkout button
                        paypalButton.setEnabled(true);
                        paypalButton.setText(R.string.checkout);
                        Toast.makeText(getApplicationContext(), "Message: " + "Transaction has been cancelled.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
