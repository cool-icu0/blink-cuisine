# Dianping

#### 介绍
这是个类似大众点评的学习小项目

#### 项目简介
该项目是基于仿大众点评的小型Java项目，旨在为用户提供一个点评和分享平台。
通过这个平台，用户可以浏览、搜索和比较各种商家的信息，包括美食、KTV等等。
用户可以在平台上创建个人账号、关注他人、发表评论、点赞、上传图片和发布自己的博客。

#### 技术介绍

##### 后端：
SpringBoot +  Redis + MyBatisPlus + Mysql
##### 前端:
Html + Axios + Vue + Nginx
##### 开发工具：
|名称|版本|
|---|---|
|JDK|1.8|
|MYSQL|8.0.25|
|Redis|6.2.6|
|Nginx|1.18.0|

#### 核心功能
1.  短信登录：Redis的共享session应用。

2.  个人主页：点赞数量、评论数量、共同关注显示功能，个人信息修改功能。

3.  粉丝、关注、共同关注：基于Set集合的关注、取关、共同消息推送等功能。

4.  在主页上查看商铺和博客：在主页上实现自动补全功能，同时提供点赞功能。

5.  附近的商户：Redis的GeoHash的应用。

6.  达人探店：基于List的点赞列表，基于SortedSet的点赞排行榜。

7.  发布评论：根据博客ID查询对应的评论树，即评论及其回复的层次结构数据。

8.  优惠券秒杀：涉及Redis的计数器、Lua脚本Redis、分布式锁、Redis消息队列等。

9.  消息通知：评论信息的显示与移除。
#### 面试相关
- **能简单介绍一下你的这个项目吗？**
    - 这个项目是一个用来降低用户踩雷概率的项目，一般我们想要一家店铺好不好吃只能自己去吃，或者去搜这个店铺，结果全是刷的评论。这个就可以通过查看网友的经验博客来判断当前这家店铺适不适合自己，比较独特的是支持店铺跟网友互动，可以发布一些自家店铺的优惠券来进行吸引顾客。
- **为什么设计他？**
    - 因为现在的这种网红店铺太多，眼花缭乱。很难找得到一家称心如意的店铺。为了减少用户试错成本，我们就可以开发一个这样的软件，但是目前来说只是实现了基本功能，无法预知那种有一定用户量的情况下是否会有问题
- **有哪些优秀的设计？**
    - 点赞功能
    - 抢购功能
    - 缓存一致性
- **遇到的最大的困难是什么？**
    - 点赞功能：该功能好的实现方案较少，大部分都是自己慢慢总结出来的。如何设计redis数据，如何设计数据库数据。如何优化接口性能都是我自己来实现的
    - 秒杀功能：涉及到的细节，知识点太多了
- **它还有什么不足？**
    - 登录功能不够完善
    - 秒杀功能，秒杀接口没有隐藏，页面静态化没有做
- **redis存储了什么数据？**
    - 在用户登录这里，使用redis缓存验证码。
    - 缓存博客数据到sortSet，做了一个博客排行榜。使用redis缓存店铺的点赞数
    - 缓存点赞数据，使用redis缓存了点赞信息
    - 在秒杀场景中，手动渲染页面进行缓存，抢购优惠券的信息，缓存接口隐藏数据，接口幂等性缓存接口信息
    - 限流注解中，缓存一个注解的限流信息
    - 唯一ID生成器，缓存唯一ID数据
    - 分布式锁，缓存锁信息
- **MQ在哪里使用了？**
    - 秒杀那里做了异步操作
    - 缓存和数据库的一致性
- **点赞排行榜是怎么设计的？**
    - 因为点赞是一个整数嘛，redi的分数是一个小数。所以我们可以给定一个固定的很大时间戳。
    - 当每一次更新的时候，我们可以拿到 (大时间戳—小时间戳)/（大时间戳）作为小数，拼接到点赞量里面
    - 这样就不仅可以获取到一个正确的点赞量，还能通过点赞量和时间进行排序
- **redis空间不足怎么办？**
    - 增加Redis的内存：可以通过修改Redis配置文件中的内存大小来增加Redis的空间。
    - 定期清理Redis中的过期数据：可以通过定期触发Redis的过期命令来清理过期的数据，以释放空间。
    - 修改内存淘汰策略：可以修改为lru，默认是不删除的策略
    - 横向扩容：可以通过分布式部署Redis来扩展Redis的空间。可以将数据分散到多个Redis实例中，以减轻单个实例的压力。
- **redis等系列场景八股？**
    - redis为什么这么快？
    - redis持久化机制？
    - redis如何保证高可用性？
    - redis内存不足了怎么办？
    - redis大key问题如何解决？
    - redis查找热key？
    - 如果想要查找一些key应该怎么实现？

