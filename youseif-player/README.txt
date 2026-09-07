Youseif Player Pro v4.4.1

Media player / PWA يحافظ على نفس الهوية البصرية مع إصلاحات استقرار ووظائف.

- يدعم MP4/WebM والصيغ التي يدعمها المتصفح، وHLS عبر hls.js أو HLS الأصلي، وDASH عند توفر dash.js.
- القنوات والمفضلة وحالة الإعدادات تحفظ محليًا؛ المفضلة تعتمد على معرف ثابت.
- الملفات المحلية تعمل خلال جلسة المتصفح فقط ما لم يتوفر تخزين دائم؛ لا يتم حفظ blob URLs كروابط دائمة.
- SRT وVTT مدعومان بعد التحويل إلى WebVTT؛ ASS/SSA وغيرها تعتمد على دعم المتصفح ولا تُعتبر مدعومة native.
- التنزيلات تتطلب رابط HTTP مباشر مع CORS صحيح؛ HLS/DASH والبث الحي لا يمكن تنزيلها كملف واحد من المتصفح.
- لا يمكن للواجهة تجاوز CORS أو تغيير User-Agent؛ عند الحاجة استخدم backend/proxy.
- autoplay وFullscreen وPiP وRemote Playback تخضع لسياسات ودعم المتصفح.
- Service Worker يخزّن app shell فقط ولا يخزن stream media أو segments.

Supported browsers: Chrome/Edge/Firefox الحديثة وSafari macOS/iOS مع قيود HLS وFullscreen وAutoplay الخاصة بكل منصة.
