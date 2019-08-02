// Examples of trying to prove the key size was set correctly on a AWS GenerateDataKeyRequest object

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import com.amazonaws.services.kms.model.DataKeySpec;
import org.checkerframework.checker.objectconstruction.qual.CalledMethods;
import org.checkerframework.checker.objectconstruction.qual.CalledMethodsPredicate;

class GenerateDataKeyRequestExamples {

    /// Interprocedural

    void callee2(AWSKMS client, @CalledMethodsPredicate("(!withNumberOfBytes) && (!setNumberOfBytes)") GenerateDataKeyRequest request) {
        request.withKeySpec(DataKeySpec.AES_256);
        client.generateDataKey(request);
    }

}
