# TV Başlatıcı (tvstarter)

Android TV ana ekran başlatıcısı. Kotlin, tek modül (`app`), view binding. Paket adı `com.tvstarter`.
Dört sekme: HOME (saat, tarih, hava durumu, değişen arka plan), APPS (favoriler + uygulamalar), LIVE (canlı TV kartı), PORTFÖY (altın/gümüş portföyü ve haberler).
Tasarım taslağı: https://claude.ai/artifact/QBPbFGCofdJVz41A625aEs

## Komutlar

Depoda gradle wrapper yok. `./gradlew` yerelde çalışmaz. CI, gradle 8.7'yi kendisi kurar (`browsy-tv` ile aynı).

- Birim testleri: `gradle testReleaseUnitTest`
- Derleme: `gradle assembleRelease`
- compileSdk 34, targetSdk 34, minSdk 21, JDK 17, Kotlin 1.9.22, AGP 8.2.2

## İş akışı

- Geliştirmeyi `claude/...` dalında yap. `main`e doğrudan push etme.
- Kırmızı derlemeyi birleştirme.
- Uygulama kodu değiştiyse `app/build.gradle` içinde `versionCode` ve `versionName` artır.
- Her `main` push'u APK'yı `build-<numara>` etiketiyle yayınlar. `claude/**` dalları yalnızca derler ve APK'yı çıktı olarak saklar.
- Commit mesajını ve PR metnini Türkçe yaz. Kullanıcıya Türkçe cevap ver.

## Mimari kararı

Android'e bağımlı olmayan mantık `logic` paketindedir (`WeatherCodes`, `WeatherParser`, `PortfolioParser`, `PortfolioHistory`, `TrFormat`, `NewsTime`, `AppListBuilder`, `WallpaperFeed`). Bu nesneler birim testiyle kapanır (`app/src/test`). Ekran kodu (`ui`) yalnızca bu nesnelerin sonucunu uygular. Ağ ve sistem erişimi `data` paketindedir.
Gezinme standart Android odak sistemidir. Sekme odak alınca seçilir (`MainActivity.selectTab`). İçerikte yukarı tuşu seçili sekmeye çıkar (`dispatchKeyEvent`). Yeni bir ekran öğesi odaklanabilir ve `bg_card` odak halkasına sahip olmalıdır. Renkleri sabit yazma, `colors.xml` belirteçlerini kullan.
Onay pencereleri ve menüler `ActionMenu` / `TextPrompt` ile kurulur. `AlertDialog` kullanma.

## Tuzaklar

- Android SDK ve Maven bu ortamdan erişilemez. Gerçek derleme ve test yalnızca CI'da olur. Kotlin yerelde yalnızca sözdizimi denetimiyle (tree-sitter) kontrol edildi, tür denetimi yapılmadı.
- HOME başlatıcısı: bazı Google TV ve Android TV 10+ cihazlar sistem ana ekranını zorunlu tutar. HOME tuşu bu cihazlarda bu uygulamaya gitmez. İlk HOME basışında sistem "her zaman" seçeneğini sorar.
- Arka plan resimleri (`WallpaperFeed`): varsayılan kaynak Bing günlük resim arşividir. Resmi API değildir ve telif koşulları belirsizdir. Kaynak değişirse yalnızca `WallpaperFeed` ve `FEED_URL` değişir. Google'ın bu iş için açık API'si yok.
- Hava durumu: konum IP adresinden bulunur (`ipwho.is`, izin gerekmez, yaklaşık şehir). Hava durumu Open-Meteo'dan gelir. İkisi de anahtarsızdır.
- LIVE: sekmede tek kart vardır ve TV'nin kendi canlı TV uygulamasını açar (`LiveTv`). Kanal listesini biz okumayız: `TvContract` ile başka uygulamaların kanallarını okumak sistem izni ister (`READ_EPG_DATA`) ve yüklenen uygulamaya verilmez. Bilinen paket adları `LiveTv.PACKAGES` ve `AndroidManifest.xml` `<queries>` içinde durur. Cihazda canlı TV uygulaması farklı bir adla gelirse iki yere de ekle. Gerçek cihazda dene.
- PORTFÖY: veri `altin-gumus-tracker` `/api/widget-data` adresinden gelir. Adres `key=` anahtarı içerir. Anahtarı depoya yazma. İki yol var: TV'de PORTFÖY sekmesinde adresi gir (cihazda saklanır), veya CI secret'ı `PORTFOLIO_URL` ver (APK'ya gömülür ve APK'dan çıkarılabilir). Sunucu geçmiş vermez: `PortfolioHistory` her başarılı yenilemede (en az 10 dk arayla) cihazda örnek biriktirir. 1G/1H/1A grafiği bu örneklerden çizilir.
- CI imza: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` secret'ları varsa release anahtarıyla imzalar. Yoksa depodaki sabit `debug.keystore` ile imzalar. Anahtar sabit olmazsa TV'de güncelleme başarısız olur. Play Store öncesi gerçek bir anahtar gerekir.
- Arayüz tek dildedir (Türkçe). Dizeler yalnızca `values/strings.xml` içindedir. `MainActivity` varsayılan yereli `tr-TR` yapar.
