// Examples of trying to prove the key size was set correctly on a AWS GenerateDataKeyRequest object

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import com.amazonaws.services.kms.model.DataKeySpec;

class GenerateDataKeyRequestExamples {

    // Legal: calls exactly one API method

    void correctWithKeySpec(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withKeySpec(DataKeySpec.AES_256);
        client.generateDataKey(request);
    }

    void correctWithNumberOfBytes(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withNumberOfBytes(32);
        client.generateDataKey(request);
    }

    void correctSetKeySpec(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.setKeySpec(DataKeySpec.AES_256);
        client.generateDataKey(request);
    }

    void correctSetNumberOfBytes(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.setNumberOfBytes(32);
        client.generateDataKey(request);
    }

    // Also legal:  with/set versions do the same thing.
    // TODO: Verify that these calls should be permitted.
    void setTwice1(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withKeySpec(DataKeySpec.AES_256);
        request.withKeySpec(DataKeySpec.AES_256);
        client.generateDataKey(request);
    }

    void setTwice2(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withKeySpec(DataKeySpec.AES_256);
        request.setKeySpec(DataKeySpec.AES_256);
        client.generateDataKey(request);
    }

    void setTwice3(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withNumberOfBytes(32);
        request.setNumberOfBytes(32);
        client.generateDataKey(request);
    }

    void setTwice4(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.setNumberOfBytes(32);
        request.setNumberOfBytes(32);
        client.generateDataKey(request);
    }


    // Illegal: fails to call a required API method

    void incorrectNoCall(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        // :: error: argument.type.incompatible
        client.generateDataKey(request);
    }

    // Illegal: calls too many API methods

    void incorrect1(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.setKeySpec(DataKeySpec.AES_256);
        request.setNumberOfBytes(32);
        // :: error: argument.type.incompatible
        client.generateDataKey(request);
    }

    void incorrect2(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withKeySpec(DataKeySpec.AES_256);
        request.setNumberOfBytes(32);
        // :: error: argument.type.incompatible
        client.generateDataKey(request);
    }

    void incorrect3(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.setKeySpec(DataKeySpec.AES_256);
        request.withNumberOfBytes(32);
        // :: error: argument.type.incompatible
        client.generateDataKey(request);
    }

    void incorrect4(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withKeySpec(DataKeySpec.AES_256);
        request.withNumberOfBytes(32);
        // :: error: argument.type.incompatible
        client.generateDataKey(request);
    }

}
