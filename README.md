<a href="#"><img src="http://storeimg.dttp.tech/images/v9Kok.png" height="256" title="SmartCookieWeb" alt="SmartCookieWeb"></a>

# BiscuitBrowser

![GitHub issues](https://img.shields.io/github/issues-raw/cookiejarapps/BiscuitBrowser)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/cookiejarapps/BiscuitBrowser)
[![Liberapay patrons](https://img.shields.io/liberapay/patrons/CookieJarApps)](https://liberapay.com/CookieJarApps)
![GitHub stars](https://img.shields.io/github/stars/cookiejarapps/BiscuitBrowser?style=social)

An experimental version of SmartCookieWeb using GeckoView and Mozilla Android Components. Adblocking, extensions and no telemetry.

---

## Support

Need help? Open an issue here, or:

- Email me at `support@cookiejarapps.com`
- Join the Matrix chat at https://matrix.to/#/#smartcookieweb:matrix.org

---

## Installation

- To get started with this project, import it into Android Studio or build from the command line with Gradle:
 
 `gradlew assembleDebug` or `./gradlew assembleDebug`

---

## Sync

Biscuit Browser can sync with Firefox. To enable sync in the mobile app, tap repeatedly on the logo in the About page, return to the settings page and then press "sign in to sync". You'll need a Firefox account, but no sync data is shared with Mozilla. Then, in Firefox for desktop, open about:config, search for `identity.sync.tokenserver.uri` and set it to `http://sync.cookiejarapps.com:5000/token/1.0/sync/1.5`, then set `identity.fxaccounts.useSessionTokensForOAuth` and `identity.sync.useOAuthForSyncToken` to `false`.

---

## Features

- Modern, clean UI
- < 4MB download
- Tons of settings
- Blocks ads and trackers

---

## Contributing

Contributions are greatly appreciated. Feel free to open an issue or pull request or help translate at (translate.cookiejarapps.com)

---


## License

- **[MPL-2.0 license](https://www.mozilla.org/en-US/MPL/2.0/)**


Copyright 2020 © CookieJarApps.

---
