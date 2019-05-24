package ru.itis.duplicates.service.impl;

import ru.itis.duplicates.dao.Dao;
import ru.itis.duplicates.dao.impl.DaoImpl;
import ru.itis.duplicates.model.ArticleWord;
import ru.itis.duplicates.model.Duplicate;
import ru.itis.duplicates.model.Library;
import ru.itis.duplicates.model.Word;
import ru.itis.duplicates.service.DuplicatesService;
import ru.itis.duplicates.util.Utils;
import ru.stachek66.nlp.mystem.holding.MyStemApplicationException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DuplicatesServiceImpl implements DuplicatesService {
    private Dao dao;

    public DuplicatesServiceImpl() {
        this.dao = new DaoImpl();
    }

    public DuplicatesServiceImpl(Dao dao) {
        this.dao = dao;
    }

    //TODO: сделать 2 потока в бд, Future<>
    @Override
    public List<Duplicate> findDuplicates(String text, String libraryUrl) {
        List<String> words;
        try {
            words = Utils.parseText(text);
        } catch (Exception e) {
            return Collections.emptyList();
        }

        if (words.size() == 0) {
            return Collections.emptyList();
        }

        List<Duplicate> doubles = new LinkedList<>();

        Library library = dao.getLibrary(libraryUrl);
        if (null != library) {
            List<String> articlesFromLibrary = dao.getArticlesFromLibrary(libraryUrl);

            if (null == articlesFromLibrary || articlesFromLibrary.isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, Long> articlesWithSignatures = dao.mapArticlesWithSignatures(articlesFromLibrary);

            List<ArticleWord> wordsFromDocument = new LinkedList<>();

            long libraryWordsCount = library.getWordsCount();

            int wordsCount = words.size();
            Map<String, Long> mapOfWordsCountForDocument = Utils.calculateWordsCount(words);
            double maxFreq = Utils.calculateMaxFrequencyInDocument(mapOfWordsCountForDocument, wordsCount);

            Map<String, Word> wordsFromLibrary = dao.getWords(words, libraryUrl);
            for (String word : mapOfWordsCountForDocument.keySet()) {
                Long wordCount = mapOfWordsCountForDocument.get(word);
                double freq = Utils.calculateFrequency(wordCount, wordsCount);
                double tf = Utils.calculateTf(freq, maxFreq);

                Word wordFromLibrary = wordsFromLibrary.get(word);

                int articlesWithWordCount = 0;
                long sumCountInCollection = 0;
                if (null != wordFromLibrary) {
                    articlesWithWordCount = wordFromLibrary.getArticlesWithWordCount();
                    sumCountInCollection = wordFromLibrary.getSumCountInCollection();
                }

                int articlesCount = articlesFromLibrary.size();

                double idf = Utils.calculateIdf(articlesWithWordCount, articlesCount);
                double sumFreq = (double) sumCountInCollection / libraryWordsCount;
                double pIdf = Utils.calculatePIdf(sumFreq, articlesCount);

                double rIdf = Utils.calculateRIdf(idf, pIdf);
                double weight = Utils.calculateWeight(tf, rIdf);

                wordsFromDocument.add(new ArticleWord(word, tf, weight));
            }
            wordsFromDocument.sort(Comparator.comparingDouble(ArticleWord::getWeight).reversed());
            List<ArticleWord> sortedMostWeightedWords = wordsFromDocument.stream().limit(6).collect(Collectors.toList());
            System.out.println(sortedMostWeightedWords);
            sortedMostWeightedWords.sort(Comparator.comparing(ArticleWord::getWord));
            StringBuilder articleStringBuilder = new StringBuilder();
            for (ArticleWord word : sortedMostWeightedWords) {
                articleStringBuilder.append(word.getWord());
            }
            String articleString = articleStringBuilder.toString();
            System.out.println(articleString);
            long articleSignature = Utils.calculateCRC32(articleString);
            System.out.println(articleSignature);

            for (String articleUrl : articlesWithSignatures.keySet()) {
                if (Long.compare(articleSignature, articlesWithSignatures.get(articleUrl)) == 0) {
                    System.out.println("Duplicates with: " + articleUrl);
                    doubles.add(new Duplicate(articleUrl));
                }
            }
        }
        return doubles;
    }

    public static void main(String[] args) throws IOException, MyStemApplicationException {
        DuplicatesService duplicatesService = new DuplicatesServiceImpl();
        /*законизображениеквадратмагазинслучайценник*/
        String s = "Горячее Лучшее Свежее Сообщества Сохраняемое Обсуждаемое 122 Для Лиги Инженеров :) Увидела картинку из TF2 и вдохновилась :) Можно было бы ещё деталей добавить, кое-что усовершенствовать, но устала уже) [моё] Печенька Лига инженеров Рисунок Team Fortress 2 77 24 1 0 0 ratataololo 1831 день назад Дубликаты не найдены Все комментарии Автора +116 NyanPyro 1687 дней назад @ratataololo +66 AlextheFOX 1829 дней назад Слу-у-ушай, я тут тоже игрок тф заядлый, могла бы ты нарисовать такого же только медика? можешь срисовать отсюда, но только печенькой =) раскрыть ветку 31 +47 ratataololo 1829 дней назад хорошо, попробую после праздников, а то уезжаю вечером к родителям) есть небольшая проблемка, я игру никогда не видела. исключительно картинки. боюсь как бы \"отсебятиной\" не испортить чего) раскрыть ветку 29 +19 Draudem 1554 дня назад если появится какая-то отсебятина, то ради тебя, эту отсебятину введут в игру в качестве очередной шляпы +16 AlextheFOX 1829 дней назад Да не бойся на этот счёт) Эта игра и так полна безумия =) Если чего не так будет на фотке, то это будет норма) +4 gadarick 1561 день назад бро, когда Медик будет? раскрыть ветку 16 +9 ratataololo 1561 день назад как раз недавно хотела заняться...но сейчас у моей семьи огромные проблемы :( не до рисунков, извини( раскрыть ветку 14 +9 gadarick 1560 дней назад ну хоть жива. ладно, спасибо что ответила, а то частенько те, кто что-то обещают пропадают или игнорируют просто. А тебе удачи, ну и успешного преодоления трудностей раскрыть ветку 13 +10 ratataololo 1560 дней назад спасибо большое) раскрыть ветку 12 +4 seanoololo 1470 дней назад ну как ты? мне на рисунок всё равно, с тобой и в семье всё хорошо?) раскрыть ветку 11 +12 ratataololo 1470 дней назад спасибо, всё наладилось) а рисунок я начала и никак не могу закончить) раскрыть ветку 10 +5 seanoololo 1470 дней назад тогда подпишусь) рад за вас) +3 Bro100Bro 1442 дня назад Здравствуйте. Надеюсь у Вас всё хорошо? Как там рисунок? раскрыть ветку 8 +12 ratataololo 1414 дней назад http://pikabu.ru/story/obeshchannyiy_million_let_nazad_peche... прошу) добила-таки) +4 ratataololo 1442 дня назад добрый день) всё хорошо, но на работе начался сезон и всё никак не доберусь до начатого) +я ленивая задница :( раскрыть ветку 6 0 Tovarishh 1414 дней назад Моё почтение, как успехи? раскрыть ветку 5 +2 ratataololo 1414 дней назад http://pikabu.ru/story/obeshchannyiy_million_let_nazad_peche... добила) раскрыть ветку 1 +1 Tovarishh 1413 дней назад Мечты сбываются, спасибо. :) +1 ratataololo 1414 дней назад как хорошо, что напомнили) нужно вечером взяться за дело) *ставит крестик на руке* раскрыть ветку 2 0 Kubik1703 1043 дня назад У Вас всё хорошо? А то все спрашивают и я хочу P.S. Рисунок отличный раскрыть ветку 1 0 ratataololo 1043 дня назад спасибо, отлично) но больше не рисую почему-то. как отрезало. очень надеюсь на выходных заставить себя намалевать обещанного пиро, ибо не дело это) 0 Nerzhavejka 630 дней назад Чувак, надо просто кнопочку \"E\" нажать))) +1 MasidaDavyn 1183 дня назад Прекрасная работа. Думал о том, чтобы поиграть часик-другой в TF2, забив на экзамены и недочитанную книгу, и спустя четверть секунды нарвался на печеньку-инженера внизу страницы Пикабу. Вот как знали) раскрыть ветку 7 +3 ratataololo 1183 дня назад Вы не представляете как приятно такое читать, будучи рукожопом, с рисованием имевшим дело только в школе) спасибо!) раскрыть ветку 6 +1 SiberiumA 1141 день назад А может, Пиро нарисуешь? раскрыть ветку 2 0 Alexwas99 656 дней назад Он уже нарисовал, глянь в профиле. раскрыть ветку 1 +1 ratataololo 656 дней назад Ну да, я рисовала и я тебе кинула :D 0 Alexwas99 658 дней назад Как здоровье? Не хочешь нарисовать Пиро (конечно же как печеньку)? раскрыть ветку 2 +1 ratataololo 656 дней назад Привет) давно готово) http://pikabu.ru/story/piropechenka_4296364 раскрыть ветку 1 0 Alexwas99 656 дней назад Ого, как оперативно) 0 Xoma163 1568 дней назад Рисуй всех :) 0 KOPACb 1281 день назад по этой игре есть серия замечательных роликов meet the ... В том числе meet the engineer и meet the medic 0 Antibolter 1148 дней назад ДАаа... Я тоже мед! Голосую за! +16 irwind 1831 день назад Похож немного) раскрыть ветку 3 +24 irwind 1831 день назад на него. раскрыть ветку 1 +12 OrangeFucker 1603 дня назад самый знаменитый инженер... +6 ratataololo 1831 день назад оригинал был брутальным) а Печенька добрый и милый :) +8 MyNameIsRob 1831 день назад отлично! и все же с руками у него какие-то проблемы раскрыть ветку 3 +5 ratataololo 1831 день назад это да. на руку с перчаткой пририсовала 5й палец, а то странно смотрелось) а до второй руки уже мои руки не дошли. уморилась) раскрыть ветку 2 +3 MyNameIsRob 1831 день назад вы молодец) а руки - это всегда проблема раскрыть ветку 1 +3 ratataololo 1831 день назад особенно, когда не умеешь рисовать :D очки для меня стали тоже огромной проблемой) +7 Shlvook 1372 дня назад Автор сделал мой день :D Умоляю, пили еще! раскрыть ветку 4 +5 ratataololo 1372 дня назад спасибо) а кого пилить?) раскрыть ветку 3 +3 Shlvook 1372 дня назад Меня лично привлекла чья-то идея сделать Медика. С другой стороны, зачем сидеть в рамках ТФ? Попробуй что-нибудь неожиданное. Печенька, символизирующая абстракционизм? Я бы за такое даже заплатил. Так или иначе, буду рад любой твоей работе:) раскрыть ветку 2 0 ratataololo 1372 дня назад спасибо, буду думать) раскрыть ветку 1 0 Shlvook 1372 дня назад Удачи! +4 Schololo 1831 день назад линии угловаты. делайте вектора прямее +3 KapitanKot 1831 день назад Сделано круто, продолжай)) раскрыть ветку 1 +2 ratataololo 1831 день назад спасибо) при наличии свободного времени и идей буду творить :) +3 Scaletta 1631 день назад Мне кажется или у него правая рука на вымя коровы похожа? раскрыть ветку 1 0 reenbic 1610 дней назад есть немного +2 Alfredpirose95 1113 дней назад Need a Dispenser here. Need a Dispenser here. Need a Dispenser here. Need a Dispenser here. Need a Dispenser here. Need a Dispenser here. +1 YellowSobaken 903 дня назад @moderator, Тег Team Fortress 2 0 Pinhead 1547 дней назад Зае*бись. 0 1max11max1 1505 дней назад Левая рука на вымя похоже раскрыть ветку 2 +2 ratataololo 1505 дней назад да они обе смахивают) раскрыть ветку 1 0 arielbka 1337 дней назад А куда пропал указательный палец? Неаккуратный инженер? 0 TurboEHOT 1505 дней назад Ребят, а версия на обои будет ? :) раскрыть ветку 2 +2 ratataololo 1505 дней назад влепить его на белый фон по размеру и готово) раскрыть ветку 1 0 TurboEHOT 1505 дней назад :D сурово Хотелось бы красивенько, как печенька сноубордист 0 ItsNotMedicine 1393 дня назад Хм, странно, без спросу поставили 0 iyuriko 1344 дня назад Красота :) 0 LarryDoul 1239 дней назад @ratataololo, сделай печеньку которая борется с раком раскрыть ветку 2 +1 ratataololo 1239 дней назад я не очень представляю как бороться с раком) или не с болезнью, а с живым?:D раскрыть ветку 1 0 DromHour 798 дней назад Можно было про живого не писать, а реализовать :D 0 muskahrun 1050 дней назад Впервые вижу печеньку, из поста с низким рейтингом, внизу страницы, причём админ сам нашёл её походу) 0 pubscrub 1036 дней назад That engineer is a bloody spy! 0 06tolik86 980 дней назад @ratataololo, а печеньку для лиги лени можно запилить? и лига рукожопов тоже была бы благодарна! раскрыть ветку 1 +1 ratataololo 980 дней назад ну рукожопа я ещё как-никак представить могу. но лига лени...печенька же будет бездействовать - ему ж всё лень) 0 Fuumarz 959 дней назад У меня вредная привычка - \"инженеров\" называть \"инжирами\". раскрыть ветку 1 0 srg.vit77 592 дня назад А ты странный... 0 drimkat4er 952 дня назад Привет, Автор! Я собираю стикеры в набор для Телеграма и было бы круто, если в мою темку ты скинул бы своих печенюх в разрешении от 512*512px и .png формате, ну а если добавишь белую обводку в 5px по контуру, то было бы просто волшебно!) Спасибо! раскрыть ветку 3 +1 ratataololo 952 дня назад дай ссылку куда кидать - вечером сделаю) раскрыть ветку 2 0 drimkat4er 952 дня назад http://pikabu.ru/story/nabor_stikerov_pikabu_dlya_telegram_4... раскрыть ветку 1 0 ratataololo 952 дня назад вроде бы все кинула) 0 Omega.Orange40 399 дней назад нужна туррель! -2 Protopups 455 дней назад Некрасивая печенька :( как попало нарисована. Больше не мультяшно, а нелепо. Даже число пальцев на руках не совпадает - и сами пальцы такие, будто приложенную к листу руку обвели. Не стильно. Вернее, она более-менее, но хуже остальных. показать ещё 0 комментариев Похожие посты Возможно, вас заинтересуют другие посты по тегам: Печенька рисунок Team Fortress 2 Авторизация Забыл пароль? Войти Регистрация Регистрация Создавая аккаунт, я соглашаюсь с правилами Пикабу и даю согласие на обработку персональных данных. Создать аккаунт Авторизация Восстановление пароля Прислать пароль Авторизация или   Если вам не приходит письмо с паролем, пожалуйста, напишите на support@pikabu.ru, указав ip-адрес, с которого вы входили в аккаунт, и посты, которые вы могли плюсовать или минусовать. Не забудьте указать сам аккаунт :) Добавить пост Комментарий дня ТОП 50 \"Моя подруга сделала это фото с рыбацкой лодки, на которой она работает в Антарктиде\" Кольца кальмара размером с покрышку камаза +907  Altay2017 20 часов назад Рекомендуемое сообщество Лига Сельского хозяйства 616 постов • 3 778 подписчиков Подписаться Все о даче, о саде и огороде и фермерстве. Активные сообщества все Строительство и ремонт Китай Всё о кино Лига фрилансеров Лига Геймеров Лига историков 1 Книжная лига 1 Лига Художников 2 Комиксы Лига путешественников 3 Создать сообщество Тенденции теги Пока что тенденции отсутствуют. Может быть, объедините теги или отредактируете их в других постах? Android Мобильная версия VK Facebook Telegram Instagram Дзен Промокоды Помощь Правила Награды Новости Пикабу Верификации Вакансии Реклама Контакты Контакты Ошибки Предложения Вакансии Реклама Информация Помощь Правила Награды Верификации Бан RSS Конфиденциальность Mobile Мобильная версия Android Партнёры Fornex.com Промокоды";
        Dao dao = new DaoImpl();
        List<String> articlesFromLibrary = dao.getArticlesFromLibrary("https://pikabu.ru");
        System.out.println(articlesFromLibrary);
        duplicatesService.findDuplicates(s, "https://pikabu.ru");
    }
}
