# Changes

## [0.14.1] - 2020-06-28

* IMPROVEMENTS
    + better theme support, especially in system-wide dark mode
* FIXES
    + restore f-droid listing icon

## [0.14.0] - 2020-06-18

* NEW
    + show transaction-level comment in transaction list
    + scroll to a specific date in the transaction list
* IMPROVEMENTS
    + better all-around theming; employ some material design recommendations
    + follow system-wide font size settings
* FIXES
    + fix a crash upon profile theme change
    + fix a crash when returning to the new transaction entry with the date
      picker open
    + various small fixes

## [0.13.1] - 2020-05-15

* additional, universal fix for entering numbers

## [0.13.0] - 2020-05-14

* NEW
    + transaction-level comment entry
    + ability to hide comment entry, per profile
* FIXES:
    + fixed crash when parsing posting flags with hledger-web before 1.14
    + visual fixes
    + fix numerical entry with some samsung keyboards

## [0.12.0] - 2020-05-06

* NEW
    + support for adding account-level comments for new transactions
    + currency/commodity support in new transaction screen, per-profile default commodity
    + control of entry dates in the future
    + support 1.14 and 1.15+ JSON API
* IMPROVEMENTS
    + darker yellow, green and cyan theme colours
    + Profiles:
        - suggest distinct color for new profiles
        - improved profile editor interface
    + avoid UI lockup while looking for a previous transaction with the chosen description
* FIXES
    + restore ability to scroll the profile details screen
    + remove profile-specific options from the database when removing a profile
    + consistent item colors in the profile details
    + fixed stuck refreshing indicator when main view is slid to the transaction list while transactions are loading
    + limit the number of launcher shortcuts to the maximum supported

## [0.11.0] - 2019-12-01

* NEW
    + new transaction: add clear button to text input fields
* SECURITY
    + avoid exposing basic HTTP authentication to wifi portals
    + profile editor: warn when using authentication with insecure HTTP scheme
    + permit cleartext HTTP traffic on Android 8+ (still, please use HTTPS to keep yout data safe while in transit)
* IMPROVEMENTS
    + clarify that crash reports are sent via email and user can review them before sending
    + allow toggling password visibility in profile details
    + reworked new transaction screen:
* FIXES
    - re-enable app shortcuts on Android 7.1 (Nougat)
    - fix possible crash when returning to new transaction screen from another app
    - fix race in setting up theme colors while creating UI
    - rotating screen no longer restarts new transaction entry
    - fix JSON API for hledger-web 1.15.2

## [0.10.3] - 2019-06-30

* FIXES:
    - JSON API parser: add String constructor for ParsedQuantity

## [0.10.2] - 2019-06-14

* FIXES:
    - two fixes in the JSON parser by Mattéo Delabre
      (for version 1.14+ hledger-web backends)

## [0.10.1] - 2019-06-05

* IMPROVEMENTS:
    - multi-color progress indicators
* FIXES:
    - avoid a crash when parsing amounts like '1,234.56'
    - show new transaction button when re-entering the app
    - use a color that is different for the new transaction submission progress
    - keep account name filter upon app re-entry
    - add MoLe version to the crash report

## [0.10.0] - 2019-05-18

* NEW:
    - profile list is a prime-time element in the side drawer, always visible
* IMPROVEMENTS
    - better app icon
    - adjust feature graphic to better fit the f-droid client's interface
    - more translations
    - more readable theme colors
    - better, smoother color selector
    - internal improvements
    - omit debug log messages in production build
    - avoid multiple acc/trn list updating when switching profiles
    - remove unused Options side drawer element
    - better "swipe up to show button" implementation, without a dummy padding row under the list
    - better async DB operations
* FIXES
    - account name filter shown only on transaction list
    - profile-dependent colors in the header items - account name filter, cancel refresh button
    - fix "synthetic" accounts created when backend skips unused accounts

## [0.9.5] - 2019-04-13

 * IMPROVEMENTS
    - nicer icon for the new transaction floating action button
 * FIXES
    - fixes in the color selection dialog, most notable on Android versions before 7

## [0.9.4] - 2019-04-13

 * FIXES
    - don't attempt to create app shortcuts (and crash) on pre 7.1 devices
    - fixed profile list expansion on pre 7.1 devices
    - fix first run experience

## [0.9.3] - 2019-04-10

 * FIXED
  - fix saving of new transactions from the app shortcut when the main app is not running

## [0.9.2] - 2019-04-08
 * FIXED
  - fix account name auto-completion when the new transaction screen is invoked by an app shortcut and the main app is not running

## [0.9.1] - 2019-04-06
 * FIXED
  - fix a crash when the new transaction screen is invoked by an app shortcut and the main app is not running

## [0.9] - 2019-04-04
 * NEW:
  - App shortcuts to the New transaction screen on Android 7.1+
  - Account list: Accounts with many commodities have their commodity list collapsed to avoid filling too much of the screen with one account
  - Account list: Viewing account's transactions migrated to a context menu
  - Auto-filling of the accounts in the new transaction screen can be limitted to the transactions using accounts corresponding to a filter -- the filter is set in the profile details
 * IMPROVED:
  - Transaction list: Back now returns to the accounts list when activated after viewing account's transactions
  - Profile details: deleting a profile requires confirmation
  - Enable animations when adding/removing rows in the new transaction screen
  - Better visual feedback when removing transaction details rows by side-swiping
  - New transactions are now sent via the JSON API if it is available
  - Better progress handling while downloading transactions via the JSON API
 * FIXED:
  - Transaction list: keep account name filter when the device is rotated
  - Avoid a restart upon app startup when the active profile uses a non-default colour theme
  - Account commodities no longer disappear after updating the data from the remote backend via the JSON API
  - Fix legacy account parser when handling missing parent accounts
  - Removed a couple of memory leaks

## [0.8.1] - 2019-03-26
 * Avoid double slashes when constructing backend URLs
 * Remove all data belonging to deleted profiles
 * Update profile list when profile list data changes
 * Fixed "has sub-accounts" internal flag when refreshing account list
 * Fix icon for f-droid
 * Cleaner color selection dialog
 * Internal reorganization of database access. Should reduce the deadlocks significantly
 * Show accumulated balance in parent accounts retrieved via the JSON API

## [0.8] - 2019-03-17
 - account list is a tree with collapsible nodes
 - account's transactions still available by tapping on amounts
 - add support for hledger-web's JSON API for retrieving accounts and transactions
 - better handling of HTTP errors
 - better display of network errors
 - some async task improvements
 - add version/API level info to the crash report

## [0.7] - 2019-03-03
 - add crash handling dialog with optional sending of the crash to the author
 - a couple of crashes fixed
 - per-profile user-selectable theme color
 - move profile list to the main navigation drawer
 - some visual glitches fixed
 - better multi-threading

## [0.6] - 2019-02-10
 - use a floating action button for the save transaction action in the new
   transaction screen
 - stop popping-up the date selection dialog when new transaction is started
 - auto-fill transaction details when a previous transaction description is
   selected

## [0.5] - 2019-02-09
 - First public release
