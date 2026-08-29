<template>
  <div class="page">
    <div class="phone">
      <div class="status-bar">
        <span>{{ currentTime }}</span>
        <div class="status-icons">
          <span class="status-signal"><i></i><i></i><i></i><i></i></span>
          <span class="status-wifi">⌁</span>
          <div class="battery"><div class="battery-fill" :style="{ width: batteryLevel + '%' }"></div></div>
        </div>
      </div>

      <div class="content">
        <transition name="view" mode="out-in">
          <div v-if="currentView === 'home'" key="home" class="view">
          <header class="header">
            <div>
              <h1>场景</h1>
              <p>按标签组合场景，告别层级结构</p>
            </div>
            <button class="icon-btn header-tag-btn" aria-label="打开卡片与标签管理" @click="openTagView">
              <span class="line-icon" v-html="uiIconSvg('tag')"></span>
            </button>
          </header>

          <div class="search-wrap">
            <span class="search-icon line-icon" v-html="uiIconSvg('search')"></span>
            <input v-model="sceneSearch" class="search-input" placeholder="搜索场景" />
            <button v-if="sceneSearch" class="search-clear" aria-label="清除搜索" @click="sceneSearch = ''" v-html="uiIconSvg('close')"></button>
          </div>

          <template v-if="searchResults !== null">
            <section class="section">
              <div class="section-title">
                <span>搜索结果 · {{ searchResults.records.length }} 个</span>
              </div>
              <div v-if="searchResults.records.length" class="list">
                <button v-for="scene in searchResults.records" :key="scene.id" class="list-item" @click="openScene(scene)">
                  <div class="emoji small">{{ scene.icon }}</div>
                  <div class="list-main">
                    <div class="title">{{ scene.name }}</div>
                    <div class="sub">{{ tagNames(scene.tags) }}</div>
                  </div>
                  <div class="count">{{ scene.checkedCount }}/{{ scene.totalCount }}</div>
                  <span class="chevron">›</span>
                </button>
              </div>
              <div v-else class="empty-tip">未找到匹配的场景</div>
            </section>
          </template>

          <template v-else>
            <section v-if="scenePinned.length" class="section">
              <div class="section-title pinned-section-title">
                <span>置顶场景</span>
                <small class="swipe-hint">左滑管理 · 长按更多</small>
                <button class="section-collapse-btn" @click="pinnedCollapsed = !pinnedCollapsed">
                  {{ pinnedCollapsed ? '展开' : '收起' }}
                </button>
              </div>
              <div v-if="!pinnedCollapsed" class="grid">
                <div v-for="(scene, idx) in scenePinned" :key="scene.id" class="swipe-row pinned-scene-row card-stagger" :style="{ animationDelay: (idx * 0.04) + 's' }"
                  @touchstart.passive="gestureStart($event, scene, 'scene')"
                  @touchend="gestureEnd($event, scene.id)"
                  @touchmove.passive="cancelLongPress"
                  @touchcancel="cancelLongPress">
                  <div class="swipe-content" :class="{ 'swipe-open': swipedItemId === scene.id }">
                    <div class="card pinned-scene-card">
                      <button class="pinned-scene-main" @click="handlePinnedSceneClick(scene)">
                        <div class="progress" :style="{ width: percent(scene) + '%' }"></div>
                        <div class="scene-icon-tile">
                          <span class="line-icon" v-html="sceneIconSvg(scene.icon)"></span>
                        </div>
                        <div class="title">{{ scene.name }}</div>
                        <div class="sub">{{ scene.checkedCount }}/{{ scene.totalCount }} 已确认</div>
                      </button>
                      <button class="pin pin-btn" aria-label="取消置顶" @click.stop="togglePin(scene)" v-html="uiIconSvg('pin')"></button>
                    </div>
                    <div class="swipe-btns">
                      <button class="swipe-btn-edit" @click.stop="openEditScene(scene)">编辑</button>
                      <button class="swipe-btn-delete" @click.stop="requestDelete('scene', scene)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <section v-if="sceneOthers.records.length || sceneOthers.total > 0" class="section">
              <div class="section-title">
                <span>其他场景</span>
                <small class="swipe-hint">左滑管理 · 长按更多</small>
              </div>
              <div class="list section-surface">
                <div v-for="scene in sceneOthers.records" :key="scene.id" class="swipe-row"
                  @touchstart.passive="gestureStart($event, scene, 'scene')"
                  @touchend="gestureEnd($event, scene.id)"
                  @touchmove.passive="cancelLongPress"
                  @touchcancel="cancelLongPress">
                  <div class="swipe-content" :class="{ 'swipe-open': swipedItemId === scene.id }">
                    <div class="list-item">
                      <button class="list-item-main" @click="handleOtherSceneClick(scene)">
                        <div class="scene-list-icon">
                          <span class="line-icon" v-html="sceneIconSvg(scene.icon)"></span>
                        </div>
                        <div class="list-main">
                          <div class="title">{{ scene.name }}</div>
                          <div class="sub">{{ tagNames(scene.tags) }}</div>
                        </div>
                        <div class="count">{{ scene.checkedCount }}/{{ scene.totalCount }}</div>
                        <span class="chevron line-icon" v-html="uiIconSvg('chevron')"></span>
                      </button>
                      <button class="pin-btn pin-inline" aria-label="置顶场景" @click.stop="togglePin(scene)" v-html="uiIconSvg('pin')"></button>
                    </div>
                    <div class="swipe-btns">
                      <button class="swipe-btn-edit" @click.stop="openEditScene(scene)">编辑</button>
                      <button class="swipe-btn-delete" @click.stop="requestDelete('scene', scene)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
              <button v-if="othersRemaining > 0" class="expand-btn" :disabled="sceneOthersLoading" @click="loadMoreScenes">
                {{ sceneOthersLoading ? '加载中...' : `加载更多 · 还剩 ${othersRemaining} 个` }}
                <span v-if="!sceneOthersLoading" class="expand-arrow">↓</span>
              </button>
              <button v-if="sceneOthers.page > 1" class="expand-btn" @click="collapseScenes">
                收起
                <span class="expand-arrow">↑</span>
              </button>
            </section>
          </template>

          <section v-show="false" v-if="searchResults === null" class="section recent-section">
            <div class="section-title">
              <span class="dot gray"></span>
              <span>最近卡片</span>
            </div>
            <div class="pill-list">
              <div v-for="card in visibleRecentCards" :key="card.id" class="pill">
                <span class="pill-text">{{ card.title }}</span>
                <div class="pill-dots">
                  <span v-for="tag in card.tags.slice(0, 2)" :key="tag.id" class="dot" :class="tag.color"></span>
                </div>
              </div>
              <div v-if="recentCards.length > 6 && !recentCardsExpanded" class="pill pill-more pill-toggle" @click="recentCardsExpanded = true">
                +{{ recentCards.length - 6 }}
              </div>
              <div v-if="recentCardsExpanded && recentCards.length > 6" class="pill pill-more pill-toggle" @click="recentCardsExpanded = false">
                收起
              </div>
            </div>
          </section>
        </div>

        <div v-else key="scene" class="view">
          <header class="header scene-header">
            <button class="icon-btn" aria-label="返回" @click="backHome" title="返回" v-html="uiIconSvg('back')"></button>
            <div style="display:flex;gap:8px">
              <button class="icon-btn" aria-label="重置" @click="resetChecks" title="重置" v-html="uiIconSvg('reset')"></button>
            </div>
          </header>
          <div v-if="showResumePrompt" class="resume-prompt">
            <span class="resume-text">
              上次已勾选 <b>{{ checkedCount }}</b> 项，还有 <b>{{ sceneCards.length - checkedCount }}</b> 项未勾选
            </span>
            <div class="resume-btns">
              <button class="resume-btn-continue" @click="showResumePrompt = false">继续上次</button>
              <button class="resume-btn-restart" @click="restartSession">重新开始</button>
            </div>
          </div>

          <div class="scene-info">
            <div class="emoji big line-icon" v-html="sceneIconSvg(selectedScene.icon)"></div>
            <div>
              <h2>{{ selectedScene.name }}</h2>
              <div class="tag-row">
                <span v-for="tag in selectedScene.tags" :key="tag.id" class="tag" :class="tag.color">{{ tag.name }}</span>
              </div>
            </div>
          </div>
          <div class="progress-row">
            <span>{{ checkedCount }}/{{ sceneCards.length }} 已确认</span>
            <span v-if="checkedCount === sceneCards.length && sceneCards.length" class="highlight">
              全部完成 <i class="completion-icon line-icon" v-html="uiIconSvg('celebrate')"></i>
            </span>
          </div>
          <div class="progress-bar">
            <div class="progress-inner" :style="{ width: sceneCards.length ? (checkedCount/sceneCards.length*100) + '%' : '0%' }"></div>
          </div>

          <div class="cards">
            <button v-for="card in sceneCards" :key="card.id" class="check-item" :class="{ done: card.checked }" @click="toggleCheck(card)">
              <div class="check-circle">{{ card.checked ? '✓' : '' }}</div>
              <span class="card-title">{{ card.title }}</span>
              <div class="pill-dots">
                <span v-for="tag in card.tags" :key="tag.id" class="dot" :class="tag.color"></span>
              </div>
            </button>
          </div>
        </div>
        </transition>
      </div>

      <div v-if="currentView === 'home'" class="bottom-nav" role="navigation" aria-label="主要导航">
        <button :class="['nav-btn', { active: !showTagView }]" @click="closeTagView">
          <span class="nav-icon line-icon" v-html="uiIconSvg('grid')"></span>
          <span>场景</span>
        </button>
        <button class="nav-btn create-nav" @click="openCreateMenu">
          <span class="fab line-icon" v-html="uiIconSvg('plus')"></span>
          <span>新建</span>
        </button>
        <button :class="['nav-btn', { active: showTagView }]" aria-label="打开管理" @click="openTagView">
          <span class="nav-icon line-icon" v-html="uiIconSvg('tag')"></span>
          <span>管理</span>
        </button>
      </div>

      <div v-if="showCreateMenu" class="modal-backdrop create-menu-backdrop" @click.self="closeCreateMenu">
        <div class="modal create-sheet">
          <div class="sheet-handle"></div>
          <div class="modal-header">
            <h3>新建</h3>
            <button class="icon-btn" aria-label="关闭" @click="closeCreateMenu" v-html="uiIconSvg('close')"></button>
          </div>
          <div class="modal-list">
            <button @click="openNewScene">
              <span class="menu-icon scene-menu-icon line-icon" v-html="uiIconSvg('layers')"></span>
              <span class="menu-copy"><strong>新建场景</strong><small>组合标签创建使用场景</small></span>
              <span class="menu-chevron line-icon" v-html="uiIconSvg('chevron')"></span>
            </button>
            <button @click="openNewCard">
              <span class="menu-icon card-menu-icon line-icon" v-html="uiIconSvg('card')"></span>
              <span class="menu-copy"><strong>新建卡片</strong><small>添加物品并打上标签</small></span>
              <span class="menu-chevron line-icon" v-html="uiIconSvg('chevron')"></span>
            </button>
            <button @click="openNewTag">
              <span class="menu-icon tag-menu-icon line-icon" v-html="uiIconSvg('tag')"></span>
              <span class="menu-copy"><strong>新建标签</strong><small>创建分类标签</small></span>
              <span class="menu-chevron line-icon" v-html="uiIconSvg('chevron')"></span>
            </button>
          </div>
        </div>
      </div>

      <div v-if="showNewCard" class="modal-backdrop top" @click.self="closeNewCard">
        <div class="modal form-sheet">
          <div class="sheet-handle"></div>
          <div class="modal-header">
            <h3>{{ editingCard ? '编辑卡片' : '新建卡片' }}</h3>
            <button class="icon-btn" aria-label="关闭" @click="closeNewCard" v-html="uiIconSvg('close')"></button>
          </div>
          <div class="form">
            <label>卡片名称</label>
            <input v-model="newCardTitle" placeholder="如：充电宝、雨伞..." />
            <label>选择标签（可多选）</label>
            <div class="chip-list">
              <button
                v-for="tag in tags"
                :key="tag.id"
                :class="['chip', selectedNewCardTags.includes(tag.id) ? tag.color : '']"
                @click="toggleNewCardTag(tag.id)"
              >
                <span v-if="selectedNewCardTags.includes(tag.id)">✓</span>{{ tag.name }}
              </button>
            </div>
            <button class="primary" :disabled="!newCardTitle || !selectedNewCardTags.length" @click="createCard">
              {{ editingCard ? '保存' : '创建卡片' }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="showNewScene" class="modal-backdrop top" @click.self="closeNewScene">
        <div class="modal form-sheet scene-form-sheet">
          <div class="sheet-handle"></div>
          <div class="modal-header">
            <h3>{{ editingScene ? '编辑场景' : '新建场景' }}</h3>
            <button class="icon-btn" aria-label="关闭" @click="closeNewScene" v-html="uiIconSvg('close')"></button>
          </div>
          <div class="form">
            <label>选择图标</label>
            <div class="emoji-list">
              <button
                v-for="emoji in emojiOptions"
                :key="emoji"
                :class="['emoji-btn', newSceneIcon === emoji ? 'active' : '']"
                @click="newSceneIcon = emoji"
                :aria-label="`选择图标 ${emoji}`"
              >
                <span class="line-icon" v-html="sceneIconSvg(emoji)"></span>
              </button>
            </div>
            <label>场景名称</label>
            <input v-model="newSceneName" placeholder="如：出门、旅行准备..." />
            <label>组合标签（场景=标签组合）</label>
            <div class="chip-list">
              <button
                v-for="tag in tags"
                :key="tag.id"
                :class="['chip', selectedNewSceneTags.includes(tag.id) ? tag.color : '']"
                @click="toggleNewSceneTag(tag.id)"
              >
                <span v-if="selectedNewSceneTags.includes(tag.id)">✓</span>{{ tag.name }}
              </button>
            </div>
            <label class="switch-row">
              <input type="checkbox" v-model="newScenePinned" /> 置顶到首页
            </label>
            <button class="primary" :disabled="!newSceneName || !selectedNewSceneTags.length" @click="createScene">
              {{ editingScene ? '保存' : '创建场景' }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="showNewTag" class="modal-backdrop top" @click.self="closeNewTag">
        <div class="modal form-sheet tag-form-sheet">
          <div class="sheet-handle"></div>
          <div class="modal-header">
            <h3>{{ editingTag ? '编辑标签' : '新建标签' }}</h3>
            <button class="icon-btn" aria-label="关闭" @click="closeNewTag" v-html="uiIconSvg('close')"></button>
          </div>
          <div class="form">
            <label>标签名称</label>
            <input v-model="newTagName" placeholder="如：日常、购物清单..." />
            <label>选择颜色</label>
            <div class="color-list">
              <button
                v-for="opt in tagColorOptions"
                :key="opt.value"
                :class="['color-btn', opt.value, newTagColor === opt.value ? 'active' : '']"
                @click="newTagColor = opt.value"
              >
                <span class="color-swatch"></span>
                <span class="color-name">{{ opt.name }}</span>
              </button>
            </div>
            <button class="primary" :disabled="!isNewTagValid" @click="createTag">{{ editingTag ? '保存' : '创建标签' }}</button>
            <div v-if="newTagError" class="form-error">{{ newTagError }}</div>
          </div>
        </div>
      </div>

      <div v-if="showTagView" class="modal-backdrop tag-view-backdrop" @click.self="closeTagView">
        <div class="modal full tag-manager">
          <div class="modal-header tag-manager-header">
            <h3>管理</h3>
            <div class="right-actions">
              <button v-if="managementTab === 'cards'" class="management-add-btn line-icon" aria-label="新建卡片" @click="openNewCard" v-html="uiIconSvg('plus')"></button>
              <button v-else class="management-add-btn line-icon" aria-label="新建标签" @click="openNewTag" v-html="uiIconSvg('plus')"></button>
              <button class="icon-btn" aria-label="关闭" @click="closeTagView" v-html="uiIconSvg('close')"></button>
            </div>
          </div>

          <div class="management-tabs" role="tablist" aria-label="管理分类">
            <button
              role="tab"
              :class="['management-tab', { active: managementTab === 'cards' }]"
              :aria-selected="managementTab === 'cards'"
              @click="managementTab = 'cards'"
            >卡片</button>
            <button
              role="tab"
              :class="['management-tab', { active: managementTab === 'tags' }]"
              :aria-selected="managementTab === 'tags'"
              @click="managementTab = 'tags'"
            >标签</button>
          </div>

          <div v-if="managementTab === 'cards'" class="management-panel">
            <div class="tag-filter">
              <button
                v-for="tag in visibleFilterTags"
                :key="tag.id"
                :class="['filter-chip', tag.color, { selected: selectedTagFilter === tag.id }]"
                @click="toggleTagFilter(tag.id)"
              >
                <b v-if="selectedTagFilter === tag.id">✓</b>{{ tag.name }} <span>{{ tag.cardCount }}</span>
              </button>
              <button v-if="tags.length > 6 && !tagFilterExpanded" class="filter-chip filter-more" @click="tagFilterExpanded = true">
                +{{ tags.length - 6 }} 个标签
              </button>
              <button v-if="tagFilterExpanded && tags.length > 6" class="filter-chip filter-more" @click="tagFilterExpanded = false">
                收起
              </button>
            </div>
            <div class="list tag-card-list">
              <div class="list-title list-title-with-hint">
                <span>{{ selectedTagFilter ? '标签下卡片' : '全部卡片' }} · {{ tagViewCards.length }} 个</span>
                <small class="swipe-hint">左滑管理 · 长按更多</small>
              </div>
              <div class="tag-card-surface">
                <div v-for="card in visibleTagViewCards" :key="card.id" class="swipe-row"
                  @touchstart.passive="gestureStart($event, card, 'card')"
                  @touchend="gestureEnd($event, card.id)"
                  @touchmove.passive="cancelLongPress"
                  @touchcancel="cancelLongPress">
                  <div class="swipe-content" :class="{ 'swipe-open': swipedItemId === card.id }">
                    <button class="list-item" @click="handleCardClick(card)">
                      <span class="card-type-icon line-icon" v-html="cardIconSvg(card.title)"></span>
                      <span class="card-text-truncate">{{ card.title }}</span>
                      <div class="tag-row">
                        <span v-for="tag in card.tags" :key="tag.id" class="tag" :class="tag.color">{{ tag.name }}</span>
                      </div>
                    </button>
                    <div class="swipe-btns">
                      <button class="swipe-btn-edit" @click.stop="openEditCard(card)">编辑</button>
                      <button class="swipe-btn-delete" @click.stop="requestDelete('card', card)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
              <button v-if="tagViewCards.length > 6 && !tagViewExpanded" class="expand-btn" @click="tagViewExpanded = true">
                展开全部 {{ tagViewCards.length }} 条
                <span class="expand-arrow">↓</span>
              </button>
              <button v-if="tagViewExpanded && tagViewCards.length > 6" class="expand-btn" @click="tagViewExpanded = false">
                收起
                <span class="expand-arrow">↑</span>
              </button>
            </div>
          </div>

          <div v-else class="management-panel tag-management-panel">
            <div class="list-title list-title-with-hint">
              <span>全部标签 · {{ tags.length }} 个</span>
              <small class="swipe-hint">左滑管理 · 长按更多</small>
            </div>
            <div class="tag-card-surface tag-manage-surface">
              <div v-for="tag in tags" :key="tag.id" class="swipe-row"
                @touchstart.passive="gestureStart($event, tag, 'tag')"
                @touchend="gestureEnd($event, tag.id)"
                @touchmove.passive="cancelLongPress"
                @touchcancel="cancelLongPress">
                <div class="swipe-content" :class="{ 'swipe-open': swipedItemId === tag.id }">
                  <button class="list-item tag-manage-row" @click="handleTagClick(tag)">
                    <span class="tag-color-dot" :class="tag.color"></span>
                    <span class="tag-manage-name">{{ tag.name }}</span>
                    <span class="tag-manage-count">{{ tag.cardCount }} 张卡片</span>
                    <span class="chevron line-icon" v-html="uiIconSvg('chevron')"></span>
                  </button>
                  <div class="swipe-btns">
                    <button class="swipe-btn-edit" @click.stop="openEditTag(tag)">编辑</button>
                    <button class="swipe-btn-delete" @click.stop="requestDelete('tag', tag)">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showActionSheet" class="action-backdrop" @click.self="showActionSheet = false">
        <div class="action-sheet">
          <div class="action-sheet-title">{{ actionSheetItem ? (actionSheetItem.name || actionSheetItem.title || '') : '' }}</div>
          <button class="action-sheet-btn" @click="handleActionEdit">编辑</button>
          <button class="action-sheet-btn danger" @click="handleActionDelete">删除</button>
          <button class="action-sheet-cancel" @click="showActionSheet = false">取消</button>
        </div>
      </div>

      <div v-if="showDeleteConfirm" class="modal-backdrop delete-confirm-backdrop" @click.self="cancelDelete">
        <div class="delete-confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-confirm-title">
          <div class="delete-confirm-icon line-icon" v-html="uiIconSvg('trash')"></div>
          <h3 id="delete-confirm-title">删除{{ deleteTypeLabel }}？</h3>
          <p>“{{ deleteTargetName }}”删除后无法恢复。</p>
          <div class="delete-confirm-actions">
            <button class="delete-cancel-btn" :disabled="deleteSubmitting" @click="cancelDelete">取消</button>
            <button class="delete-confirm-btn" :disabled="deleteSubmitting" @click="confirmDelete">
              {{ deleteSubmitting ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
import { api } from './api';
const { sceneIconSvg, uiIconSvg, cardIconSvg } = require('./ui-icons.cjs');

export default {
  name: 'App',
  data() {
    return {
      currentTime: '',
      batteryLevel: 75,
      currentView: 'home',
      sceneSearch: '',
      searchResults: null,
      searchLoading: false,
      scenePinned: [],
      sceneOthers: { records: [], total: 0, page: 0, size: 8 },
      sceneOthersLoading: false,
      tags: [],
      cardsPage: { records: [], total: 0, totalPages: 0, page: 1, size: 20 },
      selectedScene: null,
      sceneCards: [],
      showCreateMenu: false,
      showNewCard: false,
      showNewScene: false,
      showNewTag: false,
      showTagView: false,
      managementTab: 'cards',
      newCardTitle: '',
      selectedNewCardTags: [],
      newSceneName: '',
      newSceneIcon: '🎒',
      selectedNewSceneTags: [],
      newScenePinned: false,
      newTagName: '',
      newTagColor: 'bg-blue-500',
      newTagError: '',
      selectedTagFilter: null,
      tagFilterExpanded: false,
      tagViewExpanded: false,
      emojiOptions: ['🚪', '✈️', '💼', '🏃', '🌴', '🎒', '🏕️', '🎉', '📌', '📦', '🎧', '🧳', '🚲', '🧼', '📷'],
      tagColorOptions: [
        { name: '蓝色', value: 'bg-blue-500' },
        { name: '紫色', value: 'bg-purple-500' },
        { name: '绿色', value: 'bg-green-500' },
        { name: '橙色', value: 'bg-orange-500' },
        { name: '粉色', value: 'bg-pink-500' },
        { name: '红色', value: 'bg-red-500' },
        { name: '青色', value: 'bg-teal-500' },
        { name: '靛蓝', value: 'bg-indigo-500' },
        { name: '黄色', value: 'bg-yellow-500' },
        { name: '灰色', value: 'bg-gray-500' }
      ],
      recentCardsExpanded: false,
      pinnedCollapsed: false,
      showResumePrompt: false,
      swipedItemId: null,
      touchStartX: 0,
      touchStartY: 0,
      longPressTimer: null,
      showActionSheet: false,
      actionSheetItem: null,
      actionSheetType: null,
      showDeleteConfirm: false,
      deleteTarget: null,
      deleteSubmitting: false,
      gestureHandled: false,
      editingScene: null,
      editingTag: null,
      editingCard: null
    };
  },
  computed: {
    othersRemaining() {
      return Math.max(0, this.sceneOthers.total - this.sceneOthers.records.length);
    },
    visibleFilterTags() {
      return this.tagFilterExpanded ? this.tags : this.tags.slice(0, 6);
    },
    tagViewCards() {
      return this.cardsPage.records.filter(
        (c) => !this.selectedTagFilter || c.tags.some((t) => t.id === this.selectedTagFilter)
      );
    },
    visibleTagViewCards() {
      return this.tagViewExpanded ? this.tagViewCards : this.tagViewCards.slice(0, 6);
    },
    anyModal() {
      return this.showCreateMenu || this.showNewCard || this.showNewScene || this.showNewTag || this.showTagView || this.showActionSheet || this.showDeleteConfirm;
    },
    recentCards() {
      return this.cardsPage.records.slice(0, 8);
    },
    visibleRecentCards() {
      return this.recentCardsExpanded ? this.recentCards : this.recentCards.slice(0, 6);
    },
    checkedCount() {
      return this.sceneCards.filter((c) => c.checked).length;
    },
    isNewTagValid() {
      return this.newTagName.trim().length > 0;
    },
    deleteTypeLabel() {
      return { scene: '场景', card: '卡片', tag: '标签' }[this.deleteTarget?.type] || '内容';
    },
    deleteTargetName() {
      const item = this.deleteTarget?.item;
      return item ? (item.name || item.title || '') : '';
    }
  },
  watch: {
    sceneSearch(val) {
      clearTimeout(this._searchTimer);
      if (!val.trim()) {
        this.searchResults = null;
        return;
      }
      this._searchTimer = setTimeout(() => this.doSearch(val), 300);
    }
  },
  mounted() {
    this.updateTime();
    this._timeTimer = setInterval(this.updateTime, 1000);
    this.syncBattery();
    this.refreshAll();
  },
  beforeDestroy() {
    clearInterval(this._timeTimer);
  },
  methods: {
    sceneIconSvg,
    uiIconSvg,
    cardIconSvg,
    updateTime() {
      const now = new Date();
      const h = String(now.getHours()).padStart(2, '0');
      const m = String(now.getMinutes()).padStart(2, '0');
      this.currentTime = `${h}:${m}`;
    },
    async syncBattery() {
      if (!('getBattery' in navigator)) return;
      try {
        const battery = await navigator.getBattery();
        this.batteryLevel = Math.round(battery.level * 100);
        battery.addEventListener('levelchange', () => {
          this.batteryLevel = Math.round(battery.level * 100);
        });
      } catch (e) { /* keep default */ }
    },
    async refreshAll() {
      await Promise.all([this.fetchTags(), this.fetchScenes(1), this.fetchCards()]);
    },
    async fetchTags() {
      const res = await api.tags();
      this.tags = res.data || [];
    },
    async fetchCards(params = {}) {
      const res = await api.cards(Object.assign({ page: 1, size: 100 }, params));
      this.cardsPage = res.data || { records: [], total: 0, totalPages: 0, page: 1, size: 100 };
    },
    async fetchScenes(page = 1) {
      const res = await api.scenes({ page, size: this.sceneOthers.size });
      const data = res.data || {};
      this.scenePinned = data.pinned || [];
      if (page === 1) {
        this.sceneOthers.records = data.records || [];
      } else {
        this.sceneOthers.records = [...this.sceneOthers.records, ...(data.records || [])];
      }
      this.sceneOthers.total = data.total || 0;
      this.sceneOthers.page = data.page || page;
    },
    collapseScenes() {
      this.sceneOthers.records = this.sceneOthers.records.slice(0, this.sceneOthers.size);
      this.sceneOthers.page = 1;
    },
    async loadMoreScenes() {
      if (this.sceneOthersLoading) return;
      this.sceneOthersLoading = true;
      try {
        await this.fetchScenes(this.sceneOthers.page + 1);
      } finally {
        this.sceneOthersLoading = false;
      }
    },
    async doSearch(keyword) {
      this.searchLoading = true;
      try {
        const res = await api.scenes({ keyword, page: 1, size: 20 });
        const data = res.data || {};
        const all = [...(data.pinned || []), ...(data.records || [])];
        this.searchResults = { records: all, total: all.length };
      } finally {
        this.searchLoading = false;
      }
    },
    async openScene(scene) {
      const detail = await api.scene(scene.id);
      this.selectedScene = detail.data;
      const cardsRes = await api.sceneCards(scene.id);
      this.sceneCards = cardsRes.data || [];
      const checkedCards = this.sceneCards.filter(c => c.checked && c.lastCheckedAt);
      if (checkedCards.length > 0) {
        const maxTime = Math.max(...checkedCards.map(c => new Date(c.lastCheckedAt).getTime()));
        const hoursDiff = (Date.now() - maxTime) / (1000 * 60 * 60);
        this.showResumePrompt = hoursDiff > 48;
      } else {
        this.showResumePrompt = false;
      }
      this.currentView = 'scene';
    },
    backHome() {
      this.currentView = 'home';
      this.selectedScene = null;
      this.sceneCards = [];
      this.sceneSearch = '';
      this.searchResults = null;
      this.showResumePrompt = false;
    },
    async restartSession() {
      await api.resetChecks(this.selectedScene.id);
      const cardsRes = await api.sceneCards(this.selectedScene.id);
      this.sceneCards = cardsRes.data || [];
      this.showResumePrompt = false;
      this.fetchScenes();
    },
    percent(scene) {
      if (!scene.totalCount) return 0;
      return Math.round((scene.checkedCount / scene.totalCount) * 100);
    },
    tagNames(tags) {
      return (tags || []).map((t) => t.name).join(' + ');
    },
    openCreateMenu() {
      this.showCreateMenu = true;
    },
    closeCreateMenu() {
      this.showCreateMenu = false;
    },
    openNewCard() {
      this.closeCreateMenu();
      this.showNewCard = true;
    },
    closeNewCard() {
      this.showNewCard = false;
      this.newCardTitle = '';
      this.selectedNewCardTags = [];
      this.editingCard = null;
    },
    openNewScene() {
      this.closeCreateMenu();
      this.showNewScene = true;
    },
    closeNewScene() {
      this.showNewScene = false;
      this.newSceneName = '';
      this.newSceneIcon = '🎒';
      this.selectedNewSceneTags = [];
      this.newScenePinned = false;
      this.editingScene = null;
    },
    openNewTag() {
      this.closeCreateMenu();
      this.newTagName = '';
      this.newTagColor = 'bg-blue-500';
      this.newTagError = '';
      this.showNewTag = true;
    },
    closeNewTag() {
      this.showNewTag = false;
      this.newTagName = '';
      this.newTagError = '';
      this.editingTag = null;
    },
    openTagView() {
      this.showTagView = true;
      this.managementTab = 'cards';
      this.selectedTagFilter = null;
      this.swipedItemId = null;
    },
    closeTagView() {
      this.showTagView = false;
      this.swipedItemId = null;
    },
    toggleNewCardTag(id) {
      const idx = this.selectedNewCardTags.indexOf(id);
      if (idx >= 0) this.selectedNewCardTags.splice(idx, 1);
      else this.selectedNewCardTags.push(id);
    },
    toggleNewSceneTag(id) {
      const idx = this.selectedNewSceneTags.indexOf(id);
      if (idx >= 0) this.selectedNewSceneTags.splice(idx, 1);
      else this.selectedNewSceneTags.push(id);
    },
    toggleTagFilter(id) {
      this.selectedTagFilter = this.selectedTagFilter === id ? null : id;
      this.tagViewExpanded = false;
    },
    async createTag() {
      const name = this.newTagName.trim();
      if (!name) {
        this.newTagError = '请输入标签名称';
        return;
      }
      this.newTagError = '';
      if (this.editingTag) {
        await api.updateTag({ id: this.editingTag.id, name, color: this.newTagColor });
      } else {
        await api.createTag({ name, color: this.newTagColor });
      }
      this.closeNewTag();
      await this.refreshAll();
    },
    async createCard() {
      if (this.editingCard) {
        await api.updateCard({ id: this.editingCard.id, title: this.newCardTitle, tagIds: this.selectedNewCardTags });
      } else {
        await api.createCard({ title: this.newCardTitle, tagIds: this.selectedNewCardTags });
      }
      this.closeNewCard();
      await this.refreshAll();
    },
    async createScene() {
      if (this.editingScene) {
        await api.updateScene({
          id: this.editingScene.id,
          name: this.newSceneName,
          icon: this.newSceneIcon,
          pinned: this.newScenePinned,
          tagIds: this.selectedNewSceneTags
        });
      } else {
        await api.createScene({
          name: this.newSceneName,
          icon: this.newSceneIcon,
          pinned: this.newScenePinned,
          tagIds: this.selectedNewSceneTags
        });
      }
      this.closeNewScene();
      await this.fetchScenes(1);
    },
    async toggleCheck(card) {
      card.checked = !card.checked;
      await api.setCheck(this.selectedScene.id, card.id, card.checked);
      await this.fetchScenes();
    },
    async resetChecks() {
      if (!this.selectedScene) return;
      await api.resetChecks(this.selectedScene.id);
      await this.openScene({ id: this.selectedScene.id });
      await this.fetchScenes();
    },
    async togglePin(scene) {
      const payload = {
        id: scene.id,
        name: scene.name,
        icon: scene.icon,
        pinned: !scene.pinned,
        tagIds: (scene.tags || []).map((t) => t.id)
      };
      await api.updateScene(payload);
      await this.fetchScenes(1);
    },
    swipeStart(e) {
      this.touchStartX = e.touches[0].clientX;
      this.touchStartY = e.touches[0].clientY;
    },
    swipeEnd(e, id) {
      const dx = e.changedTouches[0].clientX - this.touchStartX;
      const dy = e.changedTouches[0].clientY - this.touchStartY;
      if (Math.abs(dx) > Math.abs(dy)) {
        if (dx < -50) {
          this.swipedItemId = id;
          return true;
        } else if (dx > 20) {
          this.swipedItemId = null;
          return true;
        }
      }
      return false;
    },
    gestureStart(e, item, type) {
      this.gestureHandled = false;
      this.swipeStart(e);
      this.startLongPress(item, type);
    },
    gestureEnd(e, id) {
      this.cancelLongPress();
      if (this.swipeEnd(e, id)) this.gestureHandled = true;
    },
    startLongPress(item, type) {
      this.longPressTimer = setTimeout(() => {
        this.actionSheetItem = item;
        this.actionSheetType = type;
        this.showActionSheet = true;
        this.gestureHandled = true;
      }, 500);
    },
    cancelLongPress() {
      if (this.longPressTimer) {
        clearTimeout(this.longPressTimer);
        this.longPressTimer = null;
      }
    },
    handleActionEdit() {
      this.showActionSheet = false;
      if (this.actionSheetType === 'scene') this.openEditScene(this.actionSheetItem);
      else if (this.actionSheetType === 'tag') this.openEditTag(this.actionSheetItem);
      else if (this.actionSheetType === 'card') this.openEditCard(this.actionSheetItem);
    },
    handleActionDelete() {
      this.requestDelete(this.actionSheetType, this.actionSheetItem);
    },
    requestDelete(type, item) {
      if (!item || !['scene', 'card', 'tag'].includes(type)) return;
      this.cancelLongPress();
      this.swipedItemId = null;
      this.showActionSheet = false;
      this.deleteTarget = { type, item };
      this.showDeleteConfirm = true;
    },
    cancelDelete() {
      if (this.deleteSubmitting) return;
      this.showDeleteConfirm = false;
      this.deleteTarget = null;
    },
    async confirmDelete() {
      if (!this.deleteTarget || this.deleteSubmitting) return;
      const { type, item } = this.deleteTarget;
      this.deleteSubmitting = true;
      try {
        if (type === 'scene') await this.deleteSceneItem(item);
        else if (type === 'tag') await this.deleteTagItem(item);
        else if (type === 'card') await this.deleteCardItem(item);
        this.showDeleteConfirm = false;
        this.deleteTarget = null;
      } finally {
        this.deleteSubmitting = false;
      }
    },
    openEditScene(scene) {
      this.swipedItemId = null;
      this.editingScene = scene;
      this.newSceneName = scene.name;
      this.newSceneIcon = scene.icon;
      this.selectedNewSceneTags = (scene.tags || []).map(t => t.id);
      this.newScenePinned = scene.pinned;
      this.showNewScene = true;
    },
    openEditTag(tag) {
      this.swipedItemId = null;
      this.editingTag = tag;
      this.newTagName = tag.name;
      this.newTagColor = tag.color;
      this.newTagError = '';
      this.showNewTag = true;
    },
    openEditCard(card) {
      this.swipedItemId = null;
      this.editingCard = card;
      this.newCardTitle = card.title;
      this.selectedNewCardTags = (card.tags || []).map(t => t.id);
      this.showNewCard = true;
    },
    async deleteSceneItem(scene) {
      this.swipedItemId = null;
      await api.deleteScene(scene.id);
      await this.fetchScenes(1);
    },
    async deleteTagItem(tag) {
      await api.deleteTag(tag.id);
      await this.refreshAll();
    },
    async deleteCardItem(card) {
      this.swipedItemId = null;
      await api.deleteCard(card.id);
      await this.refreshAll();
    },
    handleOtherSceneClick(scene) {
      if (this.gestureHandled) {
        this.gestureHandled = false;
        return;
      }
      if (this.swipedItemId === scene.id) {
        this.swipedItemId = null;
        return;
      }
      this.openScene(scene);
    },
    handlePinnedSceneClick(scene) {
      if (this.gestureHandled) {
        this.gestureHandled = false;
        return;
      }
      this.openScene(scene);
    },
    handleCardClick(card) {
      if (this.gestureHandled) {
        this.gestureHandled = false;
        return;
      }
      if (this.swipedItemId === card.id) {
        this.swipedItemId = null;
      }
    },
    handleTagClick(tag) {
      if (this.gestureHandled) {
        this.gestureHandled = false;
        return;
      }
      if (this.swipedItemId === tag.id) {
        this.swipedItemId = null;
        return;
      }
      this.openEditTag(tag);
    }
  }
};
</script>

