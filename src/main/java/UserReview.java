/**
 * @author Yiheng Wu
 * @date 2019/7/29
 */
public class UserReview {
    private String userName;
    private int ratingStar;
    private String comment;
    private String orderID;
    private String productName;

    public UserReview(String userName, int ratingStar, String comment, String orderID, String productName) {
        this.userName = userName;
        this.ratingStar = ratingStar;
        this.comment = comment;
        this.orderID = orderID;
        this.productName = productName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getRatingStar() {
        return ratingStar;
    }

    public void setRatingStar(int ratingStar) {
        this.ratingStar = ratingStar;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }
}
