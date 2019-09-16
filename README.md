# Shopee-Crawler
A web crawler who can fetch customer's review for some specific products searched by keywords in shopee.sg

Hello，这是一个shopee.sg(目前仅针对新加坡的shopee网站)评论爬虫小程序，只用了简单的java和jdbc数据库存取。工程管理工具用的是maven，没有使用其余的java框架。数据库使用的是SQLite（一个轻量级关系型数据库）。
如果不好安装SQLite，也可以换成别的数据库(但是代码要改)。

最简单的评论抓取是在获取了某个商品的itemID和shopID之后，就可以新建一个Item完成针对该店铺内该产品的评论抓取。抓取结果可以显示
itemID, shopID, orderID, 产品名称, 评论用户的username, 星级评价，评论内容

下面是一个例子
Item huaWei = new Item("1611921257", "998058"); //在shopee是上某个店铺中的某款华为手机
huaWei.fetchReview();
try {
    huaWei.writeReviewsToDatabase("D:\\CrawlerData\\test.db");
} catch (Exception e) {
    e.printStackTrace();
}

orderID是数据库里的唯一键约束，如果有相同的orderID的评论插入会导致插入失败！程序会退出！！！

然后还可以根据关键字搜索，searchKeyword("搜索关键字")函数会返回在shopee网站上搜索该关键字而得到的全部商品的Item（即包含它的itemID和shopID）
之后遍历该list，对每一个单独的Item执行上面的搜索过程，就可以把跟该关键字有关的全部商品的评论抓取到数据库中。

main函数里现在留存的即是搜索关键字"huawei"抓取全部商品评论的代码。
