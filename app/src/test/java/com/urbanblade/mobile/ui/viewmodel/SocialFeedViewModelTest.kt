package com.urbanblade.mobile.ui.viewmodel

import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.*
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class SocialFeedViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository
    @Before fun setup() { Dispatchers.setMain(dispatcher); repo = mock() }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun feed(vararg ids: String, pages: Int = 1) = SocialFeedResponse(ids.map { SocialWork(it) }, JsonObject().apply { addProperty("last_page", pages) })

    @Test fun `paginacion permite reaccionar y conserva la reaccion cuando llega otra pagina`() = runTest(dispatcher) {
        val nextPage = CompletableDeferred<SocialFeedResponse>()
        whenever(repo.socialFeed(1)).thenReturn(feed("a", pages = 2))
        // Suspender la consulta permite comprobar la interacción mientras sigue en curso.
        whenever(repo.socialFeed(2)).doSuspendableAnswer { nextPage.await() }
        whenever(repo.reactWork("a")).thenReturn(SocialActionResponse("added", 1))
        val vm = SocialFeedViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.loadMore(); runCurrent()
        assertTrue(vm.loadingMore.value); assertFalse(vm.busy.value)
        vm.toggleReaction("a"); runCurrent()
        assertTrue(vm.feed.value.data.single().isReacted)
        vm.load()
        nextPage.complete(feed("a", "b", pages = 2)); advanceUntilIdle()
        assertEquals(listOf("a", "b"), vm.feed.value.data.map { it.id })
        assertTrue(vm.feed.value.data.first().isReacted)
        assertFalse(vm.loadingMore.value)
        verify(repo, times(1)).socialFeed(1)
    }

    @Test fun `fallo de paginacion conserva contenido y admite reintentar`() = runTest(dispatcher) {
        whenever(repo.socialFeed(1)).thenReturn(feed("a", pages = 2))
        whenever(repo.socialFeed(2)).thenThrow(RuntimeException("sin red")).thenReturn(feed("b", pages = 2))
        val vm = SocialFeedViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        assertEquals(listOf("a"), vm.feed.value.data.map { it.id })
        assertNotNull(vm.error.value); assertFalse(vm.loadingMore.value)
        vm.loadMore(); advanceUntilIdle()
        assertNull(vm.error.value)
        assertEquals(listOf("a", "b"), vm.feed.value.data.map { it.id })
    }

    @Test fun `doble toque envia una sola reaccion y bloquea consulta mientras se envia`() = runTest(dispatcher) {
        whenever(repo.socialFeed(1)).thenReturn(feed("a"))
        whenever(repo.reactWork("a")).thenReturn(SocialActionResponse("added", 1))
        val vm = SocialFeedViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.toggleReaction("a"); vm.toggleReaction("a"); vm.load()
        assertTrue("a" in vm.pending.value)
        advanceUntilIdle()
        verify(repo, times(1)).reactWork("a")
        verify(repo, times(1)).socialFeed(1)
        assertTrue(vm.feed.value.data.single().isReacted)
        assertTrue(vm.pending.value.isEmpty())
    }

    @Test fun `fallo al comentar no borra borrador y permite reintentar`() = runTest(dispatcher) {
        whenever(repo.socialFeed(1)).thenReturn(feed("a"))
        whenever(repo.commentWork("a", "Gran corte")).thenThrow(RuntimeException("sin red")).thenReturn(PostCommentResponse(data = SocialComment(comment = "Gran corte")))
        val vm = SocialFeedViewModel(repo)
        vm.load(); advanceUntilIdle()
        var cleared = false
        vm.comment("a", " Gran corte ") { cleared = true }; advanceUntilIdle()
        assertFalse(cleared); assertNotNull(vm.error.value)
        assertEquals(0, vm.feed.value.data.single().commentsCount)
        vm.comment("a", "Gran corte") { cleared = true }; advanceUntilIdle()
        assertTrue(cleared); assertNull(vm.error.value)
        assertEquals(1, vm.feed.value.data.single().commentsCount)
    }

    @Test fun `mas trabajos no duplica publicaciones ni envia la pagina dos veces`() = runTest(dispatcher) {
        whenever(repo.socialFeed(1)).thenReturn(feed("a", pages = 2))
        whenever(repo.socialFeed(2)).thenReturn(feed("a", "b", pages = 2))
        val vm = SocialFeedViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.loadMore(); vm.loadMore(); advanceUntilIdle()
        verify(repo, times(1)).socialFeed(2)
        assertEquals(listOf("a", "b"), vm.feed.value.data.map { it.id })
        assertFalse(vm.hasMore.value)
    }

    @Test fun `reserva usa perfil de barbero y no identificador del usuario del muro`() = runTest(dispatcher) {
        whenever(repo.socialFeed(1)).thenReturn(feed("a"))
        whenever(repo.barbers()).thenReturn(listOf(BarberItem("perfil", user = BarberUser("usuario", "Barbero"))))
        val vm = SocialFeedViewModel(repo)
        vm.load(canBook = true); advanceUntilIdle()
        assertEquals("perfil", vm.bookingIds.value["usuario"])
        vm.load(); advanceUntilIdle()
        assertTrue(vm.bookingIds.value.isEmpty())
        verify(repo, times(1)).barbers()
    }

    private fun feedWithComments() = SocialFeedResponse(
        listOf(SocialWork("a", commentsCount = 2, comments = listOf(
            SocialComment(id = "c1", comment = "spam", user = SocialCommentUser("Ana")),
            SocialComment(id = "c2", comment = "hola", user = SocialCommentUser("Beto"))
        )))
    )

    @Test fun `reportar un comentario lo oculta y descuenta el contador`() = runTest(dispatcher) {
        whenever(repo.socialFeed(1)).thenReturn(feedWithComments())
        whenever(repo.reportComment("c1", "spam", null)).thenReturn(SocialModerationResponse("ok"))
        val vm = SocialFeedViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.reportComment("a", "c1", "spam"); advanceUntilIdle()
        val work = vm.feed.value.data.single()
        assertEquals(listOf("c2"), work.comments.map { it.id })
        assertEquals(1, work.commentsCount)
        assertNull(vm.error.value)
    }

    @Test fun `fallo al reportar conserva el comentario y muestra error`() = runTest(dispatcher) {
        whenever(repo.socialFeed(1)).thenReturn(feedWithComments())
        whenever(repo.reportComment("c1", "otro", null)).thenThrow(RuntimeException("sin red"))
        val vm = SocialFeedViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.reportComment("a", "c1", "otro"); advanceUntilIdle()
        assertEquals(2, vm.feed.value.data.single().comments.size)
        assertNotNull(vm.error.value)
    }

    @Test fun `bloquear al autor oculta sus comentarios del muro`() = runTest(dispatcher) {
        whenever(repo.socialFeed(1)).thenReturn(feedWithComments())
        whenever(repo.blockCommentAuthor("c2")).thenReturn(SocialModerationResponse("ok"))
        val vm = SocialFeedViewModel(repo)
        vm.load(); advanceUntilIdle()
        vm.blockCommentAuthor("a", "c2"); advanceUntilIdle()
        assertEquals(listOf("c1"), vm.feed.value.data.single().comments.map { it.id })
        verify(repo).blockCommentAuthor("c2")
    }
}
