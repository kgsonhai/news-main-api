package com.news.voicenews.bloc;

import com.news.voicenews.api.client.AudioCrawlerClient;
import com.news.voicenews.api.client.NewsCrawlerClient;
import com.news.voicenews.api.client.RankerClient;
import com.news.voicenews.dto.req.AudioCrawlerReq;
import com.news.voicenews.dto.req.FromAudioCrawlerReq;
import com.news.voicenews.dto.req.NewsCrawlerReq;
import com.news.voicenews.model.Article;
import com.news.voicenews.model.Category;
import com.news.voicenews.model.Score;
import com.news.voicenews.model.Session;
import com.news.voicenews.service.ArticleService;
import com.news.voicenews.service.CategoryService;
import com.news.voicenews.service.ScoreService;
import com.news.voicenews.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.news.voicenews.constant.CrawlerRanker.BASE_AUDIO_URL;
import static com.news.voicenews.enums.CrawlerStatus.AUDIO_CRAWLING_FAILED;
import static com.news.voicenews.enums.CrawlerStatus.AUDIO_CRAWLING_SUCCESS;
import static com.news.voicenews.enums.CrawlerStatus.FAILED;
import static com.news.voicenews.enums.CrawlerStatus.FINISHED;
import static com.news.voicenews.enums.CrawlerStatus.IN_CRAWLING_AUDIO_PROGRESS;
import static com.news.voicenews.enums.CrawlerStatus.IN_RANKER_PROGRESS;
import static com.news.voicenews.enums.CrawlerStatus.NEWS_CRAWLER_FAILED;
import static com.news.voicenews.enums.CrawlerStatus.NEWS_CRAWLER_SUCCESS;
import static com.news.voicenews.enums.CrawlerStatus.RANKER_FAILED;
import static com.news.voicenews.enums.CrawlerStatus.RANKER_SUCCESS;

@Slf4j
@Service
public class CrawlerBloc {

    private final SessionService sessionService;
    private final CategoryService categoryService;
    private final ScoreService scoreService;
    private final ArticleService articleService;
    private final AudioCrawlerClient audioCrawlerClient;
    private final NewsCrawlerClient newsCrawlerClient;
    private final RankerClient rankerClient;

    public CrawlerBloc(final SessionService sessionService,
                       final CategoryService categoryService,
                       final ScoreService scoreService,
                       final ArticleService articleService,
                       final AudioCrawlerClient audioCrawlerClient,
                       final NewsCrawlerClient newsCrawlerClient,
                       final RankerClient rankerClient) {
        this.sessionService = sessionService;
        this.categoryService = categoryService;
        this.scoreService = scoreService;

        this.articleService = articleService;
        this.audioCrawlerClient = audioCrawlerClient;
        this.newsCrawlerClient = newsCrawlerClient;
        this.rankerClient = rankerClient;
    }

    @Transactional
    public Long startCrawler() {
        log.info("Start crawler");

        Session session = sessionService.createNewSession();
        NewsCrawlerReq newsCrawlerReq = NewsCrawlerReq.builder()
                                                      .sessionId(session.getId())
                                                      .status(session.getStatus())
                                                      .build();
        newsCrawlerClient.publishSessionToNewsCrawler(newsCrawlerReq);
        return session.getId();
    }

    @Transactional
    public void updateStatusFromNewsCrawler(NewsCrawlerReq newsCrawlerReq) {
        log.info("Update status from new crawler #{}", newsCrawlerReq);

        Session session = sessionService.findById(newsCrawlerReq.getSessionId());

        if (newsCrawlerReq.getStatus() == NEWS_CRAWLER_SUCCESS) {
            session.setStatus(IN_RANKER_PROGRESS);
            rankerClient.publishSessionToRanker(newsCrawlerReq);
        } else if (newsCrawlerReq.getStatus() == NEWS_CRAWLER_FAILED) {
            log.info("News crawling failed with session id #{}", newsCrawlerReq.getSessionId());
            session.setStatus(FAILED);
        }
        sessionService.save(session);
    }

    @Async
    @Transactional
    public void updateStatusFromRanker(NewsCrawlerReq newsCrawlerReq)
            throws InterruptedException {
        log.info("Update status from ranker with news crawler #{}", newsCrawlerReq);

        Session session = sessionService.findById(newsCrawlerReq.getSessionId());

        log.info("Get status #{}", newsCrawlerReq.getStatus());


        if (newsCrawlerReq.getStatus() == RANKER_SUCCESS) {
            session.setStatus(IN_CRAWLING_AUDIO_PROGRESS);
            publishSessionToAudioCrawler(session);
        } else if (newsCrawlerReq.getStatus() == RANKER_FAILED){
            log.info("Rank failed with session id #{}", newsCrawlerReq.getSessionId());
            session.setStatus(FAILED);
        }
        sessionService.save(session);
    }

    @Transactional
    public String updateAudioPathFromAudioCrawler(FromAudioCrawlerReq fromAudioCrawlerReq) {
        log.info("Update status and audio path from audio crawler #{}", fromAudioCrawlerReq);

        List<Score> scores = scoreService.findByAllArticleId(fromAudioCrawlerReq.getUuid());

        if (fromAudioCrawlerReq.getStatus() == AUDIO_CRAWLING_SUCCESS) {
            for (Score score : scores) {
                score.setAudioPath(BASE_AUDIO_URL + fromAudioCrawlerReq.getUuid() + ".mp3");
            }
            scoreService.updateAudioPathFromAudioCrawler(scores);
        } else  if (fromAudioCrawlerReq.getStatus() == AUDIO_CRAWLING_FAILED) {
            log.info("Failed to crawl audio with article id #{}", fromAudioCrawlerReq.getUuid());
        }
        log.info("Crawl audio with article id #{}", fromAudioCrawlerReq.getUuid());
        return fromAudioCrawlerReq.getUuid();
    }

    @Async
    public void publishSessionToAudioCrawler(Session session)
            throws InterruptedException {
        log.info("Publish session to audio crawler with session #{}", session);

        List<Score> scoresShouldBeCrawlAudio = getScoresShouldBeCrawlAudio(session);

        log.info("Number of audios #{}", scoresShouldBeCrawlAudio.size());
        int i = 1;
        for (Score score : scoresShouldBeCrawlAudio) {
            log.info("Crawling audio #{}", i++);

            Article article = articleService.fetchArticleById(score.getArticleId());

            Category category = categoryService.findByName(score.getCategory());

            String content = normalizeContent(article.getTitle(), article.getContent(), category.getDescription());
            audioCrawlerClient.publishSessionToAudioCrawler(AudioCrawlerReq.builder()
                                                                           .uuid(score.getArticleId())
                                                                           .text(content)
                                                                           .build());
            int delay;
            delay = RandomUtils.nextInt(10000, 12000);
            log.info("Delay in #{} milliseconds", delay);
            TimeUnit.MILLISECONDS.sleep(delay);
        }
        this.updateScoreHasPointButAudioNull(session.getId());
        session.setStatus(FINISHED);
        session.setFinishedTime(Instant.now());
        sessionService.save(session);
        log.info("Session is finished #{}", session);
    }

    @Transactional
    public List<Score> getScoresShouldBeCrawlAudio(Session session) {
        log.info("Get scores should be crawl audio");

        List<Category> categories = categoryService.fetchAllCategories();

        List<Score> scoresShouldBeCrawlAudio = new ArrayList<>();
        for (Category category : categories) {
            List<Score> scores = scoreService.fetchScoresBySessionIdCategoryWithLimit(session.getId(),
                                                                                      category.getName(),
                                                                                      15);
            for (Score score : scores) {
                Score oldScore = scoreService.findByArticleIdAndAudioPathNotNull(score.getArticleId());
                log.info("findByArticleIdAndAudioPathNotNull #{}", oldScore);
                if (Objects.nonNull(oldScore)) {
                    scoreService.updateAudioPathByOldScorePath(score, oldScore.getAudioPath());
                    continue;
                }
                scoresShouldBeCrawlAudio.add(score);
            }
        }
        return scoresShouldBeCrawlAudio;
    }

    @Async
    public void debugCrawlAudioNotCreate()
            throws InterruptedException {
            log.info("debug Crawl Audio Not Create");

            HashMap<String, Object> fakeData = new HashMap<>();

            fakeData.put("uid", "dc2f3962-e62e-5308-9202-b153f2ed66ab");
            fakeData.put("title", "'So găng' iPhone 15 với Galaxy Z Fold5");
            fakeData.put("category", "công nghệ");
            fakeData.put("content", "Dòng smartphone mới nhất của  Apple - iPhone 15 đã gây ấn tượng với các tính năng hiện đại. Tuy nhiên  nó sẽ gặp phải thách thức lớn do các thiếu sót so với chiếc smartphone  màn hình gập Galaxy Z Fold5.\n" +
                    "                                 Galaxy  Z Fold5 nổi bật với kiểu thiết kế màn hình gập kiểu cuốn sách với cơ  chế bản lề cải tiến để tạo khoảng cách nhỏ giữa tấm nền bên trái và bên  phải khi gập, giúp nó trông hoàn toàn phẳng và song song với nhau. Mặc  dù khi gập lại, Galaxy Z Fold5 dày hơn iPhone 15 nhưng khi mở ra, nó chỉ  mỏng 6,1 mm. Ngay cả có thiết kế dạng gập lại,  Galaxy Z Fold5 vẫn cung cấp khả năng chống nước IPX8 mạnh mẽ. Galaxy Z Fold5 có màn hình gập dạng cuốn sách đẹp mắt AFP Trong  khi đó, iPhone 15 có thiết kế không tạo đột phá khi vẫn mang trên mình  kiểu truyền thống. Mặc dù Dynamic Island là một tính năng mang tính đột  phá khi ra mắt vào năm ngoái, nhiều người vẫn đánh giá kém hơn so với  kiểu màn hình đục lỗ. Đó có thể là lý do khiến  Apple dự kiến khai tử thiết kế này trong một hoặc hai năm nữa. Với  cổng USB-C, nhiều người có thể xem đây là thay đổi rất đáng xem của  iPhone 15, nhưng trên thực tế, đó là tiêu chuẩn cũ so với các sản  phẩm thuộc thế giới Android.  Galaxy Z Fold5 cung cấp đến hai màn hình, gồm màn hình ngoài 6,2 inch Dynamic AMOLED 2X cho tốc độ làm mới 48 - 120 Hz mượt mà với màu sắc tuyệt vời. Điều tương tự cũng xảy ra với màn hình trong, vốn có kích thước 7,6 inch lớn và cung cấp tốc độ làm mới 1 - 120 Hz có thể tự động chuyển đổi theo nội dung được xem. Nó cũng có máy quét dấu vân tay ở vị trí cạnh, tốc độ nhanh và đáng tin cậy. Cả màn hình trong và ngoài đều có độ phân giải cao, tốc độ làm mới 120 Hz  PhoneArena Trong  khi đó, iPhone 15 chỉ sử dụng màn hình OLED duy nhất. Ngay cả bản cao cấp nhất cũng chỉ dừng lại ở khả năng HDR và tính  năng ProMotion 120 Hz. Sản phẩm này sử dụng phương pháp mở khóa sinh  trắc học duy nhất là Face ID. Cả  Galaxy Z Fold5 và iPhone 15 đều đi kèm các chip xử lý mạnh mẽ với hiệu  suất cực kỳ tốt, đủ để chạy mượt mà mọi ứng dụng hiện đại ngày nay.  Trong khi chip A17 (trên các mẫu iPhone 15 Pro và 15 Pro Max) có  quy trình sản xuất tiên tiến 3nm lợi thế hơn Snapdragon 8 Gen 2 cho  Galaxy, không gian bộ nhớ RAM của Galaxy Z Fold5 lại ở mức 12 GB khá  thoải mái cho các nhiệm vụ đa nhiệm. Người dùng cũng có thể mở rộng bộ  nhớ trong cho Galaxy Z Fold5 nhờ khe cắm microSD, điều mà iPhone 15  không mang lại. Người dùng có thể dễ dàng biến Galaxy Z Fold5 thành PC mini di động  CNET Đặc  biệt, Galaxy Z Fold5 có thêm một lợi ích bổ sung đó là hỗ trợ DeX, cho  phép người dùng kết nối điện thoại với màn hình ngoài và thiết bị ngoại  vi để sử dụng như là một PC mini di động. Người dùng cũng có thể mua S  Pen và hộp đựng để cất bút stylus nhằm nâng cao năng suất. Thiết  lập camera của  Galaxy Z Fold5  được duy trì như tiền nhiệm nhưng vẫn là  một thiết lập mạnh mẽ với nhiều camera khác nhau. Về bản chất, điện  thoại có camera chính 50 MP hỗ trợ tự động lấy nét Dual Pixel, OIS và  khẩu độ f/1.8, kết hợp camera siêu rộng 12 MP và camera tele 10 MP. Có  đến hai camera selfie khác nhau, với một camera 10 MP đặt trong phần đục  lỗ của màn hình ngoài và một camera 4 MP đặt dưới màn hình trong. Tất  cả những điều này giúp đáp ứng mọi nhu cầu chụp ảnh của người tiêu dùng  với chiếc điện thoại này. iPhone 15 Pro cao cấp nhất chỉ có ba camera sau và một camera trước  The Verge Mặt  khác, iPhone 15 cũng rất được đánh giá cao nhờ cảm biến chính 48 MP. Tuy nhiên, ngoại trừ iPhone 15 Pro Max có camera kính tiềm vọng hoàn  toàn mới, các thay đổi với camera siêu rộng và selfie khi so sánh với  tiền nhiệm gần như không quá lớn. Bất chấp hệ sinh thái đối lập nhau, hai chiếc điện thoại này  chắc chắn trở thành lựa chọn tốt cho mọi người sử dụng. Với những ai cần  một sản phẩm kiểu dáng truyền thống, iPhone 15 có thể là cái tên lựa  chọn. Nhưng nếu là một người dùng ưa thích làn sóng đổi mới,  Galaxy Z Fold5 thú vị hơn.");


            String content = normalizeContent((String) fakeData.get("title"), (String) fakeData.get("content"), (String)fakeData.get("category"));
            log.info("debug content normalize {}: ", (String) fakeData.get("uid"));


            audioCrawlerClient.publishSessionToAudioCrawler(AudioCrawlerReq.builder()
                    .uuid((String) fakeData.get("uid"))
                    .text(content)
                    .build());
    }


    public void updateScoreHasPointButAudioNull(Long sessionId){
        List<Score> scores = scoreService.findAllAudioHasNotPath(sessionId);
        log.info("List scores have audio null #{}", scores);

//        scores = scores.subList(0,5);

            for (Score score : scores) {
                Article article = articleService.fetchArticleById(score.getArticleId());

                Category category = categoryService.findByName(score.getCategory());

                String content = normalizeContent(article.getTitle(), article.getContent(), category.getDescription());
                audioCrawlerClient.publishSessionToAudioCrawler(AudioCrawlerReq.builder()
                        .uuid(score.getArticleId())
                        .text(content)
                        .build());

                String path_audio = scoreService.findByArticleIdAndAudioPathIsNull(score.getArticleId());
                if(path_audio == null){
                    log.info("Scores has audio null  #{}", article.get_id());
                    audioCrawlerClient.publishSessionToAudioCrawler(AudioCrawlerReq.builder()
                            .uuid(score.getArticleId())
                            .text(content)
                            .build());
                }

                int delay;
                delay = RandomUtils.nextInt(10000, 12000);
                log.info("Delay in #{} milliseconds", delay);
                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
    }
    public String normalizeContent(final String articleTitle,
                                   final String articleContent,
                                   final String articleCategory) {

        String content;

        content = "Tin " + articleCategory + ". ";
        content = content + articleTitle + ". ";
        content = content + articleContent;

        content = content.replace("'", "");
        content = content.replace("\"", "");
        content = content.replace("tp", " thành phố ");
        content = content.replace("TP", " thành phố ");
        content = content.replace("HCM", "hồ chí minh");
        content = content.replace("nCoV", "covid");
        content = content.replace("T.Ư", "trung ương");
        content = content.replace("TW", "trung ương");
        content = content.replace("T.W", "trung ương");
        content = content.replace("CoV-2", "covi2");
        content = content.replace("/kg", "trên 1 kilogram");
        content = content.replace("kg", "kilogram");
        content = content.replaceAll("(\\d),(\\d)", "$1 phẩy $2");
        content = content.replaceAll("(\\d)\\.(\\d)", "$1$2");

        return content;
    }
}
