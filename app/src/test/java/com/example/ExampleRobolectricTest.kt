package com.example

import com.example.model.BOARD_SIZE
import com.example.model.BoardTile
import com.example.model.BoosterType
import com.example.model.GridPos
import com.example.model.PieceType
import com.example.viewmodel.PaperCrushViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun paperCraftCrush_initializesPlayable7x7BoardAndBoosters() {
        val viewModel = PaperCrushViewModel()
        val state = viewModel.uiState.value

        assertEquals(BOARD_SIZE, state.board.size)
        assertEquals(BOARD_SIZE, state.board.first().size)
        // Initial board should have no pre-existing 3-in-a-row matches
        assertTrue(viewModel.findMatchedPositions(state.board).isEmpty())

        // Verify Scissors, Glue, and Hammer boosters are initialized
        assertEquals(3, state.boosters[BoosterType.SCISSORS])
        assertEquals(3, state.boosters[BoosterType.GLUE])
        assertEquals(2, state.boosters[BoosterType.HAMMER])

        // Selecting a booster toggles activeBooster state
        viewModel.selectBooster(BoosterType.SCISSORS)
        assertEquals(BoosterType.SCISSORS, viewModel.uiState.value.activeBooster)
    }

    @Test
    fun paperCraftCrush_detectsHorizontalAndVerticalMatches() {
        val viewModel = PaperCrushViewModel()
        val customBoard = List(BOARD_SIZE) { r ->
            List(BOARD_SIZE) { c ->
                val type = when {
                    r == 0 && c in 0..3 -> PieceType.ORIGAMI_FOX // 4-match horizontal
                    PieceType.entries[(r * 2 + c) % (PieceType.entries.size - 1)] == PieceType.ORIGAMI_FOX ->
                        PieceType.ORIGAMI_CRANE
                    else -> PieceType.entries[(r * 2 + c) % (PieceType.entries.size - 1)]
                }
                BoardTile(id = (r * 10 + c).toLong(), type = type, row = r, col = c)
            }
        }

        val matches = viewModel.findMatchedPositions(customBoard)
        assertTrue(matches.contains(GridPos(0, 0)))
        assertTrue(matches.contains(GridPos(0, 1)))
        assertTrue(matches.contains(GridPos(0, 2)))
        assertTrue(matches.contains(GridPos(0, 3)))
    }

    @Test
    fun paperCraftCrush_all50LevelsHaveClearStageRulesAndFriendlyMoves() {
        for (lvl in 1..com.example.model.TOTAL_LEVELS) {
            val stage = com.example.model.getStageInfoForLevel(lvl)
            val blueprint = com.example.model.getLevelBlueprint(lvl)
            assertTrue(stage.easyRuleBadge.isNotBlank())
            assertTrue(blueprint.mechanicLabel.isNotBlank())
            assertTrue(blueprint.moves in 27..34)
            assertTrue(blueprint.primaryGoal.targetCount in 10..22)
        }
    }
}

