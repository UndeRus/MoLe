# Changes

## [0.21.7] = 2024-03-19

* FIXES:
    + allow user certificates in network security config
* OTHERS:
    + bump gradle version

## [0.21.6] - 2023-06-20

* FIXES
    + fix sending transations to hledger-web 1.23+

## [0.21.5] - 2022-09-03

* FIXES
    + fix cloud backup

## [0.21.4] - 2022-06-18

* FIXES
    + fix compatibility wuth hledger-web 1.23+ when submitting new transactions. Thanks to Faye Duxovni for the patch!
    + fix a crash when deleting templates
    + fix a rare crash when submitting transactions with multiple accounts with no amounts with zero remaining balance

## [0.21.3] - 2022-04-06

* FIXES
    + sync gradle version requirements
* OTHERS
    + bump version of several dependent libraries
    + bump SDK version to 31
    + adjust deprecated constructor usage

## [0.21.2] - 2022-04-04

* FIXES
    + fix crash when auto-balancing multi currency transaction
    + fix crash when duplicating template
    + fix crash when restoring configuration backup
* IMPROVEMENTS
    + new transaction: turn on commodity setting when loading previous transaction with commodities

## [0.21.1] - 2021-12-30

* FIXES
    + add hledger-web 1.23 support when adding transactions too
    + correct running total when a matching transaction is added in the past
    + fix crash when sending transaction containing only empty amounts

## [0.21.0] - 2021-12-09

* NEW
    + Add support for hledger-web 1.23
* FIXES
    + Ship database support file missed in v0.20.4

## [0.20.4] - 2021-11-18

* KNOWN PROBLEMS
    + Incompatibility with hledger-web 1.23+
* FIXES
    + fix auto-completion of transaction description

## [0.20.3] - 2021-09-29

* FIXES
    + another fix to DB migration from v0.16.0

## [0.20.2] - 2021-09-23

* NEW
    + cloud backup
* FIXES
    + two database problems fixed, one causing crashes at startup

## [0.20.1] - 2021-09-09

* FIXES
    + New transaction: focus amount upon account selection
    + New transaction: fix a crash when returning to the activity with no focused input field
    + fix a crash in DB upgrade introduced in v0.20.0
    + fix config restore with null values
    + move away from deprecated AsyncTask

## [0.20.0] - 2021-08-22

* NEW
    + backup/restore of profile/template configuration to a file
* FIXES
    + fix a couple of crashes related to starting new transaction via shortcut

## [0.19.2] - 2020-06-09

* FIXES
    + fix auto-completion of transaction names with non-ASCII characters on some Android variants/versions (broken in 0.18.0)

## [0.19.1] - 2020-05-23

* FIXES
    + fix a bug in new transaction screen when an invalid amount is entered
    + fix loading a previous transaction by description (again)
    + fix crash when parsing of hledger version with only two components

## [0.19.0] - 2020-05-10

* NEW
    + add commodity support to the templates
    + display running totals when filtering transaction list by account
    + show current balance in account chooser (new transactions)
* IMPROVEMENTS
    + more prominent background for auto-complete pop-ups in dark mode
    + better placement of account balances with very long/deep account names
* FIXES
    + honor default commodity setting in new transaction screen
    + honor changes in currently active profile
    + fix propagation of speculative account updates to parent accounts

## [0.18.0] - 2020-05-05

* NEW
    + newly added transactions are visible in transaction list without a refresh
* IMPROVEMENTS
    + finished migration to fully asynchronous database layer
    + better responsiveness when switching from the account list to the transaction list for the first time
* FIXES
    + fix layout glitches in template editor
    + fix error handling while trying different JSON API versions
    + stop resetting the date when an old transaction is loaded
    + several smaller fixes

## [0.17.1] - 2020-03-24

* FIXES
    + fix a bug in db migration for profiles without detected version

## [0.17.0] - 2020-03-11

* NEW
    + transaction templates, applied via QR scan
* IMPROVEMENTS
    + bigger commodify button in new transaction screen
    + unified floating action button behaviour
    + start migration to a fully asynchronous database layer

## [0.16.0] - 2020-12-28

* NEW
    + add support for latest JSON API (hledger-web 1.19.1)
    + backend server version detection
    + backend communication supports multiple JSON API versions
* IMPROVEMENTS
    + do database-related initialization in the background while the splash screen is shown
* FIXES
    + honour default currency in new transaction entry
    + several crashes fixed

## [0.15.0] - 2020-09-20

* NEW
    + splash screen on startup
    + show account/transaction counts
* IMPROVEMENTS
    + theme fixes, improved contrast
    + better responsiveness, more work moved to background threads
    + faster storage of retrieved data
    + last update info moved to lists to save space
* FIXES
    + fixed progress of data retrieval from hledger-web
    + fixed extra fetches of remote data
    + fill currency list with data from the journal

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
