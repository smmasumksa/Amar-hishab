# আমার হিসাব — Android Studio প্রজেক্ট

এই প্রজেক্টটি একটি প্রাথমিক Android অ্যাপের সোর্স কোড। এতে আছে:
- বাংলা UI
- SQLite-ভিত্তিক স্থানীয় ডেটাবেস (অফলাইনে লেনদেন রাখা)
- Google Sign-In
- Firestore-এ ক্লাউড ব্যাকআপ; সিঙ্কের সময় ফোন থেকে আপলোড অথবা ক্লাউড থেকে Restore বেছে নেওয়া যায়
- মাস ১২-এর ৬–১০ অক্টোবর ২০২৬-এর দেওয়া লেনদেন প্রাথমিক ডেটা হিসেবে
- JSON ব্যাকআপ Export / Import

## APK বানাতে যা লাগবে
1. Android Studio-তে এই ফোল্ডারটি Open করুন।
2. Firebase Console-এ Android app তৈরি করুন। Package name: `com.example.amarhishab`
3. Firebase Authentication-এ Google provider চালু করুন এবং Firestore Database তৈরি করুন।
4. `app/google-services.json` ফাইলটি Firebase থেকে ডাউনলোড করে `app/` ফোল্ডারে রাখুন।
5. Firebase project-এর Web client ID-টি `app/src/main/res/values/strings.xml`-এর `default_web_client_id`-এ বসান।
6. Android Studio-তে Gradle Sync করে Run/Build APK করুন।

## গুরুত্বপূর্ণ
এটি সোর্স প্রজেক্ট; এখানে APK বিল্ড বা Firebase configuration সম্পন্ন করা হয়নি। Firebase সেটআপ না করলে Google login ও cloud sync চলবে না, তবে স্থানীয় ডেটাবেস এবং হিসাবের UI কাজ করার জন্য তৈরি করা হয়েছে। প্রথমবার চালু হলে নমুনা মাস ১২-এর ডেটা SQLite-এ যোগ হবে।


## মোবাইল দিয়ে GitHub Actions-এ APK build
1. GitHub-এ নতুন repository তৈরি করে ZIP-এর ভেতরের সব ফাইল (hidden `.github` folder-সহ) upload করুন।
2. GitHub repository → Actions → `Build Amar Hishab APK` → `Run workflow` চাপুন।
3. Build সফল হলে Actions run খুলে নিচে `Artifacts` থেকে `amar-hishab-debug-apk` ডাউনলোড করুন।
4. Firebase-এর `google-services.json` এখনো যোগ না করলে এই workflow-এর build সফল হলেও অ্যাপের Google login/cloud feature কাজ করবে না। Firebase সেটআপের আগে অ্যাপটি কার্যকরভাবে ব্যবহার করার জন্য ক্লাউড বাটন ব্যবহার করবেন না।
