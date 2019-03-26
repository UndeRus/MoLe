# Changes

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
