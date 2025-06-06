<template>
  <div class="sc-message--text">
    <slot :message="message" :messageText="messageText">
      <div class="sc-message--text-content" ref="textCtxRef">
        <div v-if="cmd">
          <span style="color: aqua">{{ cmd.slice(0, 1) }}</span
          >{{ cmd.slice(1) }}
        </div>
        <div v-if="knowledgeBase" v-html="knowledgeBase"></div>
        <div ref="textRef" class="markdown-body" v-html="messageText"></div>
      </div>
    </slot>
  </div>
</template>

<script lang="ts">
import mdKatex from "@traptitech/markdown-it-katex";
// @ts-ignore
import mila from "markdown-it-link-attributes";
import hljs from "highlight.js";
import MarkdownIt from "markdown-it";
import util from "@/libs/util";
import { copyToClip } from "@/libs/copy";
import arrowUrl from "../assets/imgs/arrow1.png";

import "../assets/scss/github-markdown.scss";
import "../assets/scss/github-markdown-light.scss";
import "../assets/scss/highlight.scss";
import "../assets/scss/MarkdownMessage.scss";
import "@/styles/markdown-mcp.scss";
import { markdownItBolt } from '@/plugins/markdown-it-bolt'
import { markdownItMcp } from '@/plugins/markdown-it-mcp'
import { markdownItStock } from '@/plugins/markdown-it-stock'
const mdi = new MarkdownIt({
  html: false,
  linkify: true,
  highlight: (code: string, language: string) => {
    const validLang = !!(language && hljs.getLanguage(language));
    if (validLang) {
      const lang = language ?? "";
      return highlightBlock(
        hljs.highlight(code, { language: lang }).value,
        lang
      );
    }
    return highlightBlock(hljs.highlightAuto(code).value, "");
  },
});

mdi.use(mila, { attrs: { target: "_blank", rel: "noopener" } });
mdi.use(mdKatex, {
  blockClass: "katexmath-block rounded-md p-[10px]",
  errorColor: " #cc0000",
});
mdi.renderer.rules.fence = (tokens, idx) => {
  const t = tokens[idx];
  const code = t.content.trim();
  if (!code) {
    return '';
  }
  const res = hljs.highlightAuto(code);
  return highlightBlock(res.value, res.language);
}
mdi.use(markdownItBolt)
mdi.use(markdownItStock)
mdi.use(markdownItMcp)

function highlightBlock(str: string, lang?: string) {
  const len = str.split("\n").length;
  const maxLen = 20;

  return `<pre class="code-block-wrapper">
    <div class="code-block-header">
      <span class="code-block-header__lang">${lang}</span>
      <span class="code-block-header__diff">代码比较</span>
      <span class="code-block-header__insert">插入IDEA</span>
      <span class="code-block-header__copy">复制代码</span>
    </div>
    <code class="hljs code-block-body ${lang} hide-code show-code">${str}</code>${
    len > maxLen
      ? `<div class="code-block-footer"><img src="${arrowUrl}" class="arrow-img" /></div>`
      : ""
  }</pre>`;
}

export default {
  props: {
    message: {
      type: Object,
      required: true,
    },
    id: {
      type: Number,
      required: true,
    },
  },
  data() {
    return {
      collapseTimer: 0,
    };
  },
  computed: {
    messageText() {
      return mdi.render(this.message.data.text);
    },
    cmd() {
      if (this.message.data.cmd) {
        return this.message.data.cmd.label;
      }
      return "";
    },
    knowledgeBase() {
      if (this.message.data.knowledgeBase) {
        if (this.message.data.knowledgeBase.label.startsWith("@")) {
          return this.message.data.knowledgeBase.label.replaceAll(
            "@",
            `<span style="color: aqua;">@</span>`
          );
        }
        return `<span style="color: aqua;">${this.message.data.knowledgeBase.label.slice(
          0,
          1
        )}</span>${this.message.data.knowledgeBase.label.slice(1)}`;
      }
      return "";
    },
  },
  mounted() {
    this.addApply();
    this.addCopyEvents();
    this.addInsertEvents();
    this.addDiffEvents();
    this.collapseFn();
    this.addBoltToggleEvents();
  },
  updated() {
    this.addApply();
    this.addCopyEvents();
    this.addInsertEvents();
    this.addDiffEvents();
    this.collapseFn();
    this.bindCollapse();
    this.addBoltToggleEvents();
  },
  unmounted() {
    this.removeEvents();
    this.removeColapseEvents();
    clearTimeout(this.collapseTimer);
  },
  methods: {
    bindCollapse() {
      const currentTime = Date.now();
      clearTimeout(this.collapseTimer);
      this.collapseTimer = setTimeout(() => {
        const textRef = this.$refs.textCtxRef;
        if (textRef instanceof HTMLElement) {
          const collapseBtn = textRef.querySelectorAll(".code-block-footer");
          if (collapseBtn.length > 0) {
            collapseBtn.forEach((btn: any) => {
              const code = btn?.parentElement?.querySelector(".code-block-body");
              code?.classList.toggle("show-code");
              btn?.classList.toggle("show-all-code");
            })
          }
        }
      }, 1500);
    },
    // 展开
    collapseFn() {
      const textRef = this.$refs.textRef as HTMLDivElement;
      if (textRef) {
        const collapseBtn = textRef.querySelectorAll(".code-block-footer");
        collapseBtn.forEach((btn: any) => {
          btn.addEventListener("click", () => {
            const code = btn.parentElement.querySelector(".code-block-body");
            code.classList.toggle("show-code");
            const footer = btn.parentElement.nextElementSibling;
            btn.classList.toggle("show-all-code");
          });
        });
      }
    },
    addCopyEvents() {
      const textRef = this.$refs.textRef as HTMLDivElement;
      if (textRef) {
        const copyBtn = textRef.querySelectorAll(".code-block-header__copy");
        copyBtn.forEach((btn: any) => {
          const copyHandler = btn._copyHandler || (async () => {
            const code = btn.parentElement?.nextElementSibling?.textContent;
            if (code) {
              await copyToClip(code);
              btn.textContent = "复制成功";
              setTimeout(() => {
                btn.textContent = "复制代码";
              }, 1000);
            }
          });

          btn.removeEventListener("click", btn._copyHandler);
          btn.addEventListener("click", copyHandler);
          btn._copyHandler = copyHandler;
        });
      }
    },
    addApply() {
      this.$nextTick(() =>{
        if (this.message.data.isLast) {
          const textRef = this.$refs.textRef as HTMLDivElement;
          if (textRef) {
            const applyDivs = textRef.querySelectorAll(".code-block-header");
            applyDivs.forEach((applyDiv: any) => {
              const applyBtn = applyDiv.querySelectorAll(".code-block-header__apply");
              if (applyBtn.length === 0) {
                // Insert the Apply button
                // 创建一个新的span元素
                var spanElement = document.createElement("span");

                // 为span元素设置类名
                spanElement.className = "code-block-header__apply";

                // 设置span元素的内容
                spanElement.textContent = "Apply";

                // 将span元素追加到applyDiv中
                applyDiv.appendChild(spanElement);
              }
            });
          }
        } else {
          // Remove existing Apply buttons if isLast is false or undefined
          const textRef = this.$refs.textRef as HTMLDivElement;
          if (textRef) {
            const applyButtons = textRef.querySelectorAll(
              ".code-block-header__apply"
            );
            applyButtons.forEach((button) => button.remove());
          }
        }
        this.addApplyEvents();
      })
    },
    addApplyEvents() {
      const textRef = this.$refs.textRef as HTMLDivElement;
      if (textRef) {
        const applyBtn = textRef.querySelectorAll(".code-block-header__apply");
        applyBtn.forEach((btn: any) => {
          const applyHandler = btn._applyHandler || (() => {
            util.applyCode();
          });

          btn.removeEventListener("click", btn._applyHandler);
          btn.addEventListener("click", applyHandler);
          btn._applyHandler = applyHandler;
        });
      }
    },
    addDiffEvents() {
      const textRef = this.$refs.textRef as HTMLDivElement;
      if (textRef) {
        const insertBtn = textRef.querySelectorAll(".code-block-header__diff");
        insertBtn.forEach((btn: any) => {
          btn.onclick = async () => {
            const code = btn.parentElement?.nextElementSibling?.textContent;
            if (code) {
              try {
                await util.diffCode(code);
                btn.textContent = "代码比较打开中";
              } catch (e) {
                btn.textContent = "代码比较";
              }
              setTimeout(() => {
                btn.textContent = "代码比较";
              }, 1000);
            }
          };
        });
      }
    },
    addInsertEvents() {
      const textRef = this.$refs.textRef as HTMLDivElement;
      if (textRef) {
        const insertBtn = textRef.querySelectorAll(
          ".code-block-header__insert"
        );
        insertBtn.forEach((btn: any) => {
          const insertHandler = btn._insertHandler || (async () => {
            const code = btn.parentElement?.nextElementSibling?.textContent;
            if (code) {
              try {
                await util.insertCode(code);
                btn.textContent = "插入IDEA成功";
              } catch (e) {
                btn.textContent = "插入IDEA失败";
              }
              setTimeout(() => {
                btn.textContent = "插入IDEA";
              }, 1000);
            }
          });

          btn.removeEventListener("click", btn._insertHandler);
          btn.addEventListener("click", insertHandler);
          btn._insertHandler = insertHandler;
        });
      }
    },
    addBoltToggleEvents() {
      const textRef = this.$refs.textRef as HTMLDivElement;
      if (textRef) {
        // 先移除旧的事件监听器
        const toggleBtns = textRef.querySelectorAll('.bolt-action-header-toggle');
        toggleBtns.forEach((btn: any) => {
          // 存储事件处理函数的引用
          const toggleHandler = btn._toggleHandler || (() => {
            console.log("toggleHandler");
            const content = btn.parentElement.querySelector('.bolt-action-content');
            content.classList.toggle('expanded');
            const toggleIcon = btn.querySelector('.bolt-action-toggle');
            toggleIcon.classList.toggle('expanded');
          });

          // 先移除旧的事件(如果存在)
          btn.removeEventListener('click', toggleHandler);
          // 添加新的事件
          btn.addEventListener('click', toggleHandler);

          // 将handler存储在DOM元素上,方便之后移除
          btn._toggleHandler = toggleHandler;
        });
      }
    },
    removeEvents() {
      const textRef = this.$refs.textRef as HTMLDivElement;
      if (textRef) {
        // 移除copy事件
        const copyBtn = textRef.querySelectorAll(".code-block-header__copy");
        copyBtn.forEach((btn: any) => {
          if (btn._copyHandler) {
            btn.removeEventListener("click", btn._copyHandler);
            delete btn._copyHandler;
          }
        });

        // 移除apply事件
        const applyBtn = textRef.querySelectorAll(".code-block-header__apply");
        applyBtn.forEach((btn: any) => {
          if (btn._applyHandler) {
            btn.removeEventListener("click", btn._applyHandler);
            delete btn._applyHandler;
          }
        });

        // 移除insert事件
        const insertBtn = textRef.querySelectorAll(".code-block-header__insert");
        insertBtn.forEach((btn: any) => {
          if (btn._insertHandler) {
            btn.removeEventListener("click", btn._insertHandler);
            delete btn._insertHandler;
          }
        });

        // 删除apply绑定事件
        const applyButtons = textRef.querySelectorAll(
          ".code-block-header__apply"
        );
        applyButtons.forEach((button) => button.remove());
        const diffBtn = textRef.querySelectorAll(".code-block-header__diff");
        diffBtn.forEach((btn: any) => {
          btn.onclick = null;
        });
        // 正确移除bolt toggle事件
        const toggleBtns = textRef.querySelectorAll('.bolt-action-header-toggle');
        toggleBtns.forEach((btn: any) => {
          if (btn._toggleHandler) {
            btn.removeEventListener('click', btn._toggleHandler);
            delete btn._toggleHandler;
          }
        });
      }
    },
    removeColapseEvents() {
      const textRef = this.$refs.textRef as HTMLDivElement;
      if (textRef) {
        const copyBtn = textRef.querySelectorAll(".code-block-footer");
        copyBtn.forEach((btn: any) => {
          btn.removeEventListener("click", () => {});
        });
        const insertBtn = textRef.querySelectorAll(
          ".code-block-header__insert"
        );
        insertBtn.forEach((btn: any) => {
          btn.onclick = null;
        });
      }
    },
  },
};
</script>

<style scoped lang="scss">
.sc-message--text {
  // padding: 5px 20px;
  // border-radius: 6px;
  font-weight: 300;
  font-size: 14px;
  line-height: 1.4;
  position: relative;
  -webkit-font-smoothing: subpixel-antialiased;

  .sc-message--text-content {
    white-space: pre-wrap;
    line-height: 2;
  }

  &:hover .sc-message--toolbox {
    left: -20px;
    opacity: 1;
  }

  .sc-message--toolbox {
    transition: left 0.2s ease-out 0s;
    white-space: normal;
    opacity: 0;
    position: absolute;
    left: 0px;
    width: 25px;
    top: 0;

    button {
      background: none;
      border: none;
      padding: 0px;
      margin: 0px;
      outline: none;
      width: 100%;
      text-align: center;
      cursor: pointer;

      &:focus {
        outline: none;
      }
    }

    & :deep(svg) {
      margin-left: 5px;
    }
  }

  code {
    font-family: "Courier New", Courier, monospace !important;
  }
}

.sc-message--content.sent .sc-message--text {
  color: white;
  background-color: #4e8cff;
  max-width: calc(100% - 120px);
  word-wrap: break-word;
}

.sc-message--content.received .sc-message--text {
  color: #263238;
  background-color: #f4f7f9;
  margin-right: 40px;
}

a.chatLink {
  color: inherit !important;
}
</style>
