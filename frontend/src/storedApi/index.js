import {
  restGet,
  restPatchMultiplePartForm,
  restPost,
  restPatch,
  restPostMultiplePartForm,
  restPostWithHtmlResponse,
} from '../restful/restful';

const storedApi = (store) => {
  function loadReviewPointViewedByUser(data) {
    if (!data) return;
    const { noteWithPosition, linkViewedbyUser } = data;
    if (noteWithPosition) {
      store.commit('loadNotes', [noteWithPosition.note]);
    }
    if (linkViewedbyUser) {
      loadReviewPointViewedByUser({
        noteWithPosition: linkViewedbyUser.sourceNoteWithPosition,
      });
      loadReviewPointViewedByUser({
        noteWithPosition: linkViewedbyUser.targetNoteWithPosition,
      });
    }
  }

  async function updateTextContentWithoutUndo(noteId, noteContentData) {
    const { updatedAt, ...data } = noteContentData;
    const res = await restPatchMultiplePartForm(
      `/api/text_content/${noteId}`,
      data,
    );
    store.commit('loadNotes', [res]);
    return res;
  }

  return {
    reviewMethods: {
      async getOneInitialReview() {
        const res = await restGet(`/api/reviews/initial`);
        loadReviewPointViewedByUser(res);
        return res;
      },

      async doInitialReview(data) {
        const res = await restPost(`/api/reviews`, data);
        loadReviewPointViewedByUser(res);
        return res;
      },

      async selfEvaluate(reviewPointId, data) {
        const res = await restPost(
          `/api/reviews/${reviewPointId}/self-evaluate`,
          data
        );
        loadReviewPointViewedByUser(res.reviewPointViewedByUser);
        return res;
      },

      async getNextReviewItem() {
        const res = await restGet(`/api/reviews/repeat`);
        loadReviewPointViewedByUser(res.reviewPointViewedByUser);
        return res;
      },
    },

    async getNoteWithDescendents(noteId) {
      const res = await restGet(`/api/notes/${noteId}/overview`);
      store.commit('loadNotes', res.notes);
      return res;
    },

    async getNoteAndItsChildren(noteId) {
      const res = await restGet(`/api/notes/${noteId}`);
      store.commit('loadNotes', res.notes);
      return res;
    },

    async getNotebooks() {
      const res = await restGet(`/api/notebooks`);
      store.commit('notebooks', res.notebooks);
      return res;
    },

    async createNotebook(circle, data) {
      const url = (() => {
        if (circle) {
          return `/api/circles/${circle.id}/notebooks`;
        }
        return `/api/notebooks/create`;
      })();

      const res = await restPostMultiplePartForm(url, data);
      return res;
    },

    async createNote(parentId, data) {
      const res = await restPostMultiplePartForm(
        `/api/notes/${parentId}/create`,
        data
      );
      store.commit('loadNotes', res.notes);
      return res;
    },

    async createLink(sourceId, targetId, data) {
      const res = await restPost(
        `/api/links/create/${sourceId}/${targetId}`,
        data
      );
      store.commit('loadNotes', res.notes);
      return res;
    },

    async updateLink(linkId, data) {
      const res = await restPost(`/api/links/${linkId}`, data);
      store.commit('loadNotes', res.notes);
      return res;
    },

    async deleteLink(linkId) {
      const res = await restPost(`/api/links/${linkId}/delete`, {});
      store.commit('loadNotes', res.notes);
      return res;
    },

    async updateNote(noteId, noteContentData) {
      const { updatedAt, ...data } = noteContentData;
      const res = await restPatchMultiplePartForm(`/api/notes/${noteId}`, data);
      store.commit('loadNotes', [res]);
      return res;
    },

    async updateTextContent(noteId, noteContentData) {
      store.commit('addEditingToUndoHistory', { noteId });
      return updateTextContentWithoutUndo(noteId, noteContentData);
    },

    async addCommentToNote(noteId, commentContentData) {
      const { updatedAt, ...data } = commentContentData;
      const res = await restPost(
        `/api/comments/${noteId}/add`,
        data,
        () => null
      );
      store.commit('loadComments', [res]);
      return res;
    },

    async undo() {
      const history = store.getters.peekUndo();
      store.commit('popUndoHistory');
      if (history.type === 'editing') {
        return updateTextContentWithoutUndo(
          history.noteId,
          history.textContent
        );
      }
      const res = await restPatch(
        `/api/notes/${history.noteId}/undo-delete`,
        {}
      );
      store.commit('loadNotes', res.notes);
      if (res.notes[0].parentId === null) {
        this.getNotebooks(store);
      }
      return res;
    },

    async deleteNote(noteId) {
      const res = await restPost(`/api/notes/${noteId}/delete`, {}, () => null);
      store.commit('deleteNote', noteId);
      return res;
    },

    async getCurrentUserInfo() {
      const res = await restGet(`/api/user/current-user-info`);
      store.commit('currentUser', res.user);
      return res;
    },

    async updateUser(userId, data) {
      const res = await restPatchMultiplePartForm(`/api/user/${userId}`, data);
      store.commit('currentUser', res);
      return res;
    },

    async createUser(data) {
      const res = await restPostMultiplePartForm(`/api/user`, data);
      store.commit('currentUser', res);
      return res;
    },

    getFeatureToggle() {
      return (
        !window.location.href.includes('odd-e.com') &&
        restGet(`/api/testability/feature_toggle`).then((res) =>
          store.commit('featureToggle', res)
        )
      );
    },

    async setFeatureToggle(data) {
      const res = await restPost(`/api/testability/feature_toggle`, {
        enabled: data,
      });
      this.getFeatureToggle(store);
      return res;
    },

    getCircle(circleId) {
      return restGet(`/api/circles/${circleId}`);
    },
  };
};

const api = () => ({
    userMethods: {
      logout() {
        return restPostWithHtmlResponse(`/logout`, {});
      },

      currentUser() {
          return restGet(`/api/user`);
      },
    },
    reviewMethods: {
      processAnswer(reviewPointId, data) {
        return restPost(`/api/reviews/${reviewPointId}/answer`, data);
      },

      removeFromReview(reviewPointId) {
        return restPost(`/api/review-points/${reviewPointId}/remove`, {});
      },

      overview() {
          return restGet(`/api/reviews/overview`);
      },

      getReviewSetting(noteId) {
          return restGet(`/api/notes/${noteId}/review-setting`);
      },

      updateReviewSetting(noteId, data) {
          return restPost(`/api/notes/${noteId}/review-setting`, data);
      }
    },
    circleMethods: {
        createCircle(data) {
            return restPostMultiplePartForm("/api/circles", data);
        },
        joinCircle(data) {
            return restPostMultiplePartForm( `/api/circles/join`, data)
        },
        getCirclesOfCurrentUser() {
            return restGet("/api/circles");
        },
    },

    relativeSearch(noteId, {searchGlobally, searchKey}) {
        return restPost(
          `/api/notes/${noteId}/search`,
          { searchGlobally, searchKey })
    },

    updateNotebookSettings(notebookId, data) {
      return restPostMultiplePartForm(
        `/api/notebooks/${notebookId}`, data
      )
    },

    getBazaar() {
        return restGet("/api/bazaar");
    },
    shareToBazaar(notebookId) {
        return restPost( `/api/notebooks/${notebookId}/share`, {})
    },

    getFailureReports() {
        return restGet("/api/failure-reports");
    },
    getFailureReport(failureReportId) {
        return restGet(`/api/failure-reports/${failureReportId}`);
    },
    subscriptionMethods: {
        subscribe(notebookId, data) {
        return restPostMultiplePartForm(
            `/api/subscriptions/notebooks/${notebookId}/subscribe`, data
        )
        },
        updateSubscription(subscriptionId, data) {
        return restPostMultiplePartForm(
            `/api/subscriptions/${subscriptionId}`, data
        )
        },
        deleteSubscription(subscriptionId) {
            return restPost(
            `/api/subscriptions/${subscriptionId}/delete`,
            {},
            )
        },
    },
  })

export { api, storedApi };
