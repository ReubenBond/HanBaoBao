# HànBǎoBāo
Mandarin Chinese text segmentation and mobile dictionary Android app (中文分词)

I wrote this app to assist myself in learning Mandarin.

This repository consists of two parts:
  * A floating dictionary Android app which segments, transliterates, and provides dictionary definitions for Chinese text (simplified & traditional)
  * A program for building the database used by that app

**Features**:
  * **Text Segmentation** - split sentences into individual words. Tap a word multiple times to re-split.
  * **Transliteration** - transliterate words into their Pinyin representation.
  * **Dictionary Definitions** - tapping a word opens a list of dictionary definitions (CCEDict, NTI Buddhist Dictionary, ADSO, others).
  * **Tone Markings** - words are marked with their tone using both glyphs over the pinyin and colorization.
  * **Tap to Read** - tap on text in your chat app to load it into HanBaoBao.
  * **Hide by HSK Level** - optionally hide transliteration for all words below a given HSK level.
  * **Part of Speech Tags** - many words have part-of-speech and ontology tags.
  * **Translation Tool** - drag the icon into the translation tool to translate the sentence using Microsoft Translator or Google Translate (if installed)

The database building program compiles data from many sources and outputs a SQLite db which is read by the Android app.
The database is likely useful for creating other apps and services.

The text segmentation algorithm used in the app is a custom one, but it works fairly well for my purposes, particularly since segments (words) can be resegmented by tapping on them.

Here's an older version of the app in action: https://www.youtube.com/watch?v=a9x9MBoLfxs

The app needs work to support Android 8 and some of the dictionary data is out-dated.

The dictionary data contained within is presented without license: obtain usage permission as needed.
