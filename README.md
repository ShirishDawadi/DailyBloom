# Daily Bloom

An Android app that delivers a fresh image and inspirational quote every day — right on your home screen as a widget.

---

## Features

### Home Screen Widget
- Displays a new image and quote every day
- Auto-refreshes at midnight via background scheduling
- Manual refresh available from the app

### Home Fragment
- View today's image and quote
- Manually change the image and quote with a button

### Profile Fragment
- Calendar view to browse past entries
- Tap any date to see the image and quote from that day

---

## Demo

> [Watch screen recording](demo/demo.mp4)


https://github.com/user-attachments/assets/419a84e0-1a45-4798-af80-a1c9bd916af0


---

## Tech Stack

| Library | Purpose |
|---|---|
| Firebase Auth | User authentication |
| Firebase Firestore | Cloud data storage |
| WorkManager | Background midnight refresh scheduling |
| Glide | Image loading and caching |
| Material Calendar View | Calendar UI for past entries |

---

## Getting Started

### Prerequisites
- Android Studio
- Java SDK
- A Firebase project with Auth and Firestore enabled

### Installation

```bash
# Clone the repo
git clone https://github.com/ShirishDawadi/DailyBloom.git

# Open in Android Studio
# Add your google-services.json file to the app/ directory
# Build and run
```

---

## Roadmap

- [ ] Export favorite entries
- [ ] Custom themes for the widget
- [ ] Notification reminders

---

## Author

**Shirish Dawadi**
[GitHub](https://github.com/ShirishDawadi) • [Email](mailto:shirishdawadi1@gmail.com)
