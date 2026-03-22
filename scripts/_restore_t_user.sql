-- MySQL dump 10.13  Distrib 8.4.8, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: campus_love
-- ------------------------------------------------------
-- Server version	8.4.8

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `t_user`
--

/*!40000 ALTER TABLE `t_user` DISABLE KEYS */;
INSERT INTO `t_user` (`id`, `email`, `password`, `nickname`, `gender`, `birth_date`, `birth_time`, `school`, `major`, `grade`, `activity_score`, `user_level`, `is_admin`, `mbti`, `zodiac`, `bazi`, `avatar_url`, `bio`, `interests`, `profile_complete`, `status`, `created_at`, `updated_at`, `last_follower_viewed_at`, `last_feed_activity_viewed_at`, `last_invite_activity_viewed_at`, `feed_visibility`, `feed_visibility_time`, `invite_count`, `participate_count`, `credit_score`, `moment_photo_url`, `moment_self_score`, `moment_banned`, `cover_image_url`, `bazi_unknown`, `ice_break_enabled`, `ai_disclosure_settings`, `deleted_at`, `delete_reason`, `moment_priority_count`) VALUES (1,'2400101027@mails.szu.edu.cn','$2a$10$cE5AExe7QfwuZGxyzroD/un2ZkbgiB6wbsK5MyKwbovd.xhSXMd4a','shyguy',1,'2003-03-18',NULL,'深圳大学','计算机','研二',272,3,0,'ISFJ','双鱼座','癸未','/uploads/avatar_1_620464c1.jpg','shyguy','篮球,足球,钢琴,吉他,音乐,读书,健身,游戏,动漫,追剧,Cosplay,手账,天文,心理学',1,1,NULL,'2026-03-07 21:15:56','2026-03-10 14:49:04','2026-03-10 15:58:22','2026-03-10 15:58:18','ALL',-1,2,1,100,'/uploads/moment_1_1772889356086.jpg',5,0,NULL,0,0,'{\"mbti\": true, \"zodiac\": true, \"baziInfo\": false, \"interestTags\": true, \"majorCategory\": true, \"naturalLangTags\": false, \"questionnaireHints\": false}',NULL,NULL,0),(2,'001@mails.szu.edu.cn','$2a$10$i9SrNuyBcTv2JVQOnt8s1.kvwNTSyVe4KLjRQ8SfVchH0MOiwe/N2','shyguy1',2,'2001-03-04',NULL,'深圳大学','美术','大三',305,4,0,'INTP','双鱼座','辛巳',NULL,'哈哈哈','羽毛球,追剧,动漫,宠物,咖啡,游戏,手账,唱歌,瑜伽,音乐',1,1,NULL,'2026-03-07 21:54:44','2026-03-20 21:36:41','2026-03-20 21:37:01','2026-03-20 21:36:50','ALL',-1,1,1,100,'/uploads/moment_2_1772891684633.jpg',9,0,NULL,0,0,'{\"mbti\": true, \"zodiac\": true, \"baziInfo\": false, \"interestTags\": true, \"majorCategory\": true, \"naturalLangTags\": false, \"questionnaireHints\": false}',NULL,NULL,0),(3,'002@mails.szu.edu.cn','$2a$10$HCMzjT6u7TV4KsBFmCz1NeXE3qqHxrwrX8eb.1Cm3Br3dtUyZitNy','shyguy2',2,'2004-01-01',NULL,'深圳大学','艺术专业','大四',14,1,0,'ENFJ','摩羯座','甲申',NULL,'嘿嘿嘿','足球,舞蹈,追剧,手账,心理学,天文,植物,宠物',1,1,NULL,NULL,'2026-03-10 14:52:16','2026-03-10 14:52:05','2026-03-10 14:52:04','ALL',-1,0,0,100,NULL,NULL,0,NULL,0,0,'{\"mbti\": true, \"zodiac\": true, \"baziInfo\": false, \"interestTags\": true, \"majorCategory\": true, \"naturalLangTags\": false, \"questionnaireHints\": false}',NULL,NULL,0),(4,'003@mails.szu.edu.cn','$2a$10$odWa7xIIePPDwO3YSquvcOkx9Fu4Bm1aUFgokpMVxKrVUpxyTyaKG','shyguy3',1,'2007-05-30','21:11:00','南方科技大学','计算机','大四',38,1,0,'ENFP','双子座','丁亥 (亥时生)',NULL,'哈哈哈','足球,天文,心理学,手账,追剧,动漫,瑜伽,健身,游戏',1,1,NULL,'2026-03-20 21:37:54','2026-03-20 21:38:46','2026-03-20 21:38:46','2026-03-20 21:38:46','ALL',-1,0,0,100,NULL,NULL,0,NULL,0,0,'{\"mbti\": true, \"zodiac\": true, \"baziInfo\": false, \"interestTags\": true, \"majorCategory\": true, \"naturalLangTags\": false, \"questionnaireHints\": false}',NULL,NULL,0),(5,'004@mails.szu.edu.cn','$2a$10$AmiuBBM8G7k7C/213HPB8uoQ2KQM/CqdGwIhhTFexXLvcHnHtTvo6','aaa',1,'2003-07-02',NULL,'深圳大学','计算机','研二',6333,10,1,'ISFP','巨蟹座','癸未','/uploads/avatar_5_1773561166568_012de703.png','哈哈哈','Cosplay,舞蹈,羽毛球,动漫,吉他,钢琴,植物,天文',1,1,NULL,'2026-03-07 21:53:51','2026-03-20 14:58:21','2026-03-20 21:38:20','2026-03-20 16:08:18','ALL',-1,1,0,100,'/uploads/moment_5_1772891631065.jpg',8,0,NULL,0,0,'{\"mbti\": true, \"zodiac\": true, \"baziInfo\": false, \"interestTags\": true, \"majorCategory\": true, \"naturalLangTags\": false, \"questionnaireHints\": false}',NULL,NULL,1),(6,'005@mails.szu.edu.cn','$2a$10$wbaN0H1ZIt1FyO.eBwwJS.J0ay8L3lULc3Ch/Q0U4eI4lPFqqnSMC','shyguy',2,'2026-03-18','15:35:00','北京大学','艺术','研一',15,1,0,'ESTJ','双鱼座','丙午 (申时生)',NULL,'哈哈哈哈哈','Cosplay,舞蹈,书法,辩论,烘焙,哲学,足球,摄影,游泳',1,1,NULL,NULL,NULL,'2026-03-07 15:36:54','2026-03-07 15:36:01','ALL',-1,0,0,100,NULL,NULL,0,NULL,0,0,'{\"mbti\": true, \"zodiac\": true, \"baziInfo\": false, \"interestTags\": true, \"majorCategory\": true, \"naturalLangTags\": false, \"questionnaireHints\": false}',NULL,NULL,0);
/*!40000 ALTER TABLE `t_user` ENABLE KEYS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-22  0:14:05
