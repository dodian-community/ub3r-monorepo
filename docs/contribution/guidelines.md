# Contributing Guidelines

These are the guidelines we'd like you to follow if you choose to contribute to our codebase. While they might seem a little demanding, it can be quite the task to manage community contributions if there were no guidelines. So to keep everyone's sanity, we request that you read these carefully before submitting any code or issues.

_**Also**, on behalf of the Dodian community in its entirety; thanks for taking an interest in contributing towards our codebase._

---

<details>
<summary>Navigation Menu</summary>

<ul>
    <li><a href="/docs/contribution">Guides</a>
        <ul>
            <li><a href="/docs/guides/getting_started.md">Getting Started</a></li>
            <li><a href="/docs/guides/installing_mysql.md">Installing MySQL Database</a></li>
            <li style="margin-top: 5px"><a href="/docs/guides/glossary.md">Glossary</a></li>
        </ul>
    </li>
    <li><a href="/docs/contribution">Contribution</a>
        <ul>
            <li><a href="/docs/contribution/guidelines.md">Contribution Guidelines</a></li>
            <li><a href="/docs/contribution/issue_definitions.md">Issue Definitions</a></li>
        </ul>
    </li>
    <li><a href="/docs/development">Development</a>
        <ul>
            <li><a href="/docs/development/database.md">Ub3r Database</a></li>
        </ul>
    </li>
    <li><a href="/docs/other">Other</a>
        <ul>
            <li><a href="/docs/other/environment_variables.md">Environment Variables</a></li>
        </ul>
    </li>
</ul>

</details>

---

## Getting Started Developing

1. Read the rest of this document
2. [Create a fork](https://github.com/dodian-community/ub3r-monorepo/fork) of our repository
3. Follow the steps from _[Setting Up Development Environment](/docs/README.md#setting-up-development-environment)_

## Submitting Issues to the Repository

_**For the sake of clarity, an issue is a ticket.**_

When submitting issues _(can be feature requests, game content ideas, bug reports, or anything else related to the game-server and/or -client)_, you need to be as descriptive as possible, so it's easy to manage the issues. Failing to do so might mean it takes a lot more time to handle your issue.

**See our [Issue Definitions](/docs/contribution/issue_definitions.md)-guidelines for more info on how to format them.**

## What to Contribute?

The best way to help is to first look at our [open issues](https://github.com/dodian-community/ub3r-monorepo/issues), and see if you can find something there you'd like to finish off. However, if what you have in mind isn't an open issue in the issue tracker, best course of action would be to create it. This way you ensure that it's something we'll actually accept into the official repository in the first place.

## Commits & Pull Requests

When sending us [pull requests][glossary-pr-explanation], please do note that every [pull request][glossary-pr-explanation] needs to be manually checked before it can be accepted. This means if you have a lot of changes, it will take a significant amount of time. So make sure your commits have decently explaining messages, and to only submit commits related to one feature per [pull request][glossary-pr-explanation].

If you happen to work on multiple features, you will have to separate them into feature branches. Example if you're working on feature A and bug B and C; you should have to branch those off in 3 different branches, submitting each set of commits in [pull request][glossary-pr-explanation] separately.

You may create [pull requests][glossary-pr-explanation] before something is complete, however it needs to be a draft. This way we can see that something is being worked on, you could also ask questions, and by it being a draft we know it's a work in progress.

## Before Submitting [Pull Requests][glossary-pr-explanation]

Before submitting your [pull request][glossary-pr-explanation], you need to make sure the project is in as good shape as before you started, and still compiles. Below is a general checklist you should follow before submitting.

- [ ] Make sure you don't have any spelling mistakes in your changes. _(To the best of your abilities anyway)_
- [ ] Make sure you use the same conventions that we've already used. _(Project is currently a mess, so do to the best of your abilities)_
  - _We'll make this easier in the future, by adding some configuration files, and/or link to specifications that defines the conventions we use._
- [ ] Make sure you've followed the guidelines in the section above _([Commits & Pull Requests](#commits--pull-requests))_.
  - If you've reformatted big sections or whole code files, please submit this in a separate PR and branch.
- [ ] Make sure the project still compiles using Gradle. If it doesn't, you'll need to fix this before submitting a [PR][glossary-pr-explanation] for review.
- [ ] Make sure there are no new errors or warnings present during the build process.



[glossary-pr-explanation]: /docs/guides/glossary.md#