# MGI Movement Register — Android App

এই project-টি `MGI Movement Register-2026.xlsx`-কে ভিত্তি করে তৈরি একটি dependency-free Android app।

## ফিচার
- Performance Dashboard
- Monthly Register আলাদা করে দেখা
- Route Search
- গুরুত্বপূর্ণ ফাংশনের আলাদা button
- Dashboard-এর 3-dot menu
- 3-dot menu-তে Import XLSX / Refresh / About
- XLSX Backup এবং Restore
- Android file picker-এর মাধ্যমে Google Drive-এ backup/restore করা যায়
- App exit confirmation: হ্যাঁ / না
- মূল XLSX `assets`-এ bundled; প্রথমবার app চালু হলে internal storage-এ copy হয়
- Restore/Import করার আগে XLSX parse করে যাচাই করা হয়; parse না হলে মূল workbook replace হয় না

## Google Drive backup কীভাবে কাজ করে
Backup/Restore Android-এর standard document picker ব্যবহার করে। Backup চাপলে file picker খুলবে; সেখানে Google Drive নির্বাচন করে ফাইল save করা যাবে। Restore চাপলে Drive থেকে `.xlsx` বেছে নেওয়া যাবে। এভাবে আলাদা Google Drive API key বা Firebase configuration দরকার হয় না।

## GitHub দিয়ে APK build
1. এই পুরো project folder GitHub repository-তে upload করুন।
2. `Actions` tab খুলুন।
3. `Build Android APK` workflow run করুন অথবা নতুন commit push করুন।
4. Workflow শেষ হলে `MGI-Movement-Register-debug-apk` artifact থেকে `app-debug.apk` নিন।

## Android Studio
Project root folder Android Studio-তে open করে Gradle sync করুন। `app` module থেকে Debug APK build করা যাবে।

## গুরুত্বপূর্ণ নোট
- এই source project-এ Google Drive SDK নয়, Android Storage Access Framework ব্যবহার করা হয়েছে। তাই Drive account selection/permission Android/Google Drive app-এর picker দ্বারা পরিচালিত হয়।
- Release APK publish করতে নিজের signing key ব্যবহার করা উচিত।
