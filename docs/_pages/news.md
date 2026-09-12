---
title: 'News'
permalink: /news/
toc: false
---

Release notes and updates from the Snow-White project.

<p>
  <a href="{{ '/feed.xml' | relative_url }}"><i class="fas fa-rss"></i>&nbsp; Subscribe via RSS</a>
</p>

<div class="news-listing">
{% for post in site.posts %}
  <div class="news-listing__entry" style="margin-bottom: 2rem;">
    <h2 style="margin-bottom: 0.25rem;"><a href="{{ post.url | relative_url }}">{{ post.title }}</a></h2>
    <p class="page__meta" style="margin-top: 0;">
      <i class="far fa-clock"></i> {{ post.date | date: "%B %-d, %Y" }}
    </p>
    <p>{{ post.excerpt }}</p>
  </div>
{% endfor %}
</div>
