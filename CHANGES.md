# Changes

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
