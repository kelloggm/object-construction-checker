import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;

// https://nvd.nist.gov/vuln/detail/CVE-2018-15869
public class Cve {
    private static final String IMG_NAME = "some_linux_img";

    public static void main(String args[]) {
        // Should not be allowed unless .withOwner is also used
        DescribeImagesResult result = client.describeImages(new DescribeImagesRequest()
                .withFilters(new Filter("name").withValues(IMG_NAME)));

    }

}
