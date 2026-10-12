FUTURE FACE SCANNER — Android Studio project

यह एक शुरुआती Android ऐप प्रोजेक्ट है:
- कैमरे से लाइव वीडियो
- ML Kit से face detection (चेहरे की जगह ढूँढ़ना; व्यक्ति का नाम/पहचान नहीं)
- Neon HUD और animated scanning line
- प्रोसेसिंग डिवाइस पर होती है; यह ऐप किसी व्यक्ति की पहचान या निजी जानकारी नहीं निकालता।

APK बनाने के लिए:
1. कंप्यूटर पर Android Studio इंस्टॉल करें।
2. Android Studio में इस ZIP को Extract करके Open करें (FutureFaceScanner फ़ोल्डर चुनें)।
3. Gradle sync पूरा होने दें; इंटरनेट चाहिए ताकि dependencies डाउनलोड हो सकें।
4. Android फोन में Developer options और USB debugging चालू करें, फोन USB से जोड़ें।
5. Run ▶ दबाकर फोन पर इंस्टॉल करें।
6. APK फ़ाइल बनाने के लिए: Build > Build Bundle(s) / APK(s) > Build APK(s).
   बनने पर Android Studio में "Locate" से APK फ़ाइल खोल सकते हैं।

महत्वपूर्ण:
- इस ZIP में तैयार APK नहीं है; यह ऐप का source project है, जिसे Android Studio से build करना होगा।
- कैमरा अनुमति केवल face detection के लिए माँगी जाती है।
- यह सुरक्षा/हैकिंग टूल नहीं है; केवल visual scanner UI और face detection demo है।
- Face box की position अलग-अलग कैमरा aspect ratios पर approximate हो सकती है।
