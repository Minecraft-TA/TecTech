package com.github.technus.tectech.thing.metaTileEntity.multi;

import static com.github.technus.tectech.TecTech.eyeOfHarmonyRecipeStorage;
import static com.github.technus.tectech.thing.casing.GT_Block_CasingsTT.texturePage;
import static com.github.technus.tectech.thing.casing.TT_Container_Casings.eyeOfHarmonyRenderBlock;
import static com.github.technus.tectech.thing.casing.TT_Container_Casings.sBlockCasingsBA0;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.GT_HatchElement.InputBus;
import static gregtech.api.enums.GT_HatchElement.InputHatch;
import static gregtech.api.enums.GT_HatchElement.OutputBus;
import static gregtech.api.enums.GT_HatchElement.OutputHatch;
import static gregtech.api.enums.GT_Values.AuthorColen;
import static gregtech.api.util.GT_StructureUtility.buildHatchAdder;
import static gregtech.api.util.GT_Utility.formatNumbers;
import static java.lang.Math.*;
import static net.minecraft.util.EnumChatFormatting.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.github.technus.tectech.recipe.EyeOfHarmonyRecipe;
import com.github.technus.tectech.thing.block.TileEyeOfHarmony;
import com.github.technus.tectech.thing.casing.TT_Container_Casings;
import com.github.technus.tectech.thing.metaTileEntity.multi.base.GT_MetaTileEntity_MultiblockBase_EM;
import com.github.technus.tectech.thing.metaTileEntity.multi.base.render.TT_RenderedExtendedFacingTexture;
import com.github.technus.tectech.util.CommonValues;
import com.github.technus.tectech.util.ItemStackLong;
import com.google.common.collect.ImmutableList;
import com.google.common.math.LongMath;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;

import appeng.util.ReadableNumberConverter;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Materials;
import gregtech.api.enums.MaterialsUEVplus;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IGlobalWirelessEnergy;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Input;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.common.tileentities.machines.GT_MetaTileEntity_Hatch_OutputBus_ME;
import gregtech.common.tileentities.machines.GT_MetaTileEntity_Hatch_Output_ME;

@SuppressWarnings("SpellCheckingInspection")
public class GT_MetaTileEntity_EM_EyeOfHarmony extends GT_MetaTileEntity_MultiblockBase_EM
        implements IConstructable, IGlobalWirelessEnergy, ISurvivalConstructable {

    public static final boolean EOH_DEBUG_MODE = false;
    private static final long MOLTEN_SPACETIME_PER_FAILURE_TIER = 14_400L;
    private static final double SPACETIME_FAILURE_BASE = 2;
    private static final String TOOLTIP_BAR = GOLD
            + "------------------------------------------------------------------------------------";

    // Region variables.
    private static Textures.BlockIcons.CustomIcon ScreenOFF;
    private static Textures.BlockIcons.CustomIcon ScreenON;

    private int spacetimeCompressionFieldMetadata = -1;
    private int timeAccelerationFieldMetadata = -1;
    private int stabilisationFieldMetadata = -1;

    private static final double SPACETIME_CASING_DIFFERENCE_DISCOUNT_PERCENTAGE = 0.03;
    private static final double TIME_ACCEL_DECREASE_CHANCE_PER_TIER = 0.1;
    // % Increase in recipe chance and % decrease in yield per tier.
    private static final double STABILITY_INCREASE_PROBABILITY_DECREASE_YIELD_PER_TIER = 0.05;
    private static final double LOG_BASE_CONSTANT = Math.log(4.4);

    private static final int TOTAL_CASING_TIERS_WITH_POWER_PENALTY = 8;

    private String userUUID = "";
    private long euOutput = 0;

    private long startEU = 0;

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, IItemSource source, EntityPlayerMP actor) {
        if (mMachine) return -1;
        int realBudget = elementBudget >= 200 ? elementBudget : Math.min(200, elementBudget * 5); // 200 blocks max per
                                                                                                  // placement.
        return survivialBuildPiece(STRUCTURE_PIECE_MAIN, stackSize, 16, 16, 0, realBudget, source, actor, false, true);
    }

    protected static final String STRUCTURE_PIECE_MAIN = "main";

    // Multiblock structure.
    private static final IStructureDefinition<GT_MetaTileEntity_EM_EyeOfHarmony> STRUCTURE_DEFINITION = IStructureDefinition
            .<GT_MetaTileEntity_EM_EyeOfHarmony>builder()
            .addShape(
                    STRUCTURE_PIECE_MAIN,
                    transpose(
                            new String[][] {
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "               C C               ", "               C C               ",
                                            "               C C               ", "            CCCCCCCCC            ",
                                            "               C C               ", "            CCCCCCCCC            ",
                                            "               C C               ", "               C C               ",
                                            "               C C               ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "               C C               ",
                                            "               C C               ", "               C C               ",
                                            "               C C               ", "              DDDDD              ",
                                            "             DDCDCDD             ", "         CCCCDCCDCCDCCCC         ",
                                            "             DDDDDDD             ", "         CCCCDCCDCCDCCCC         ",
                                            "             DDCDCDD             ", "              DDDDD              ",
                                            "               C C               ", "               C C               ",
                                            "               C C               ", "               C C               ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "               C C               ",
                                            "               C C               ", "               C C               ",
                                            "                D                ", "                D                ",
                                            "             DDDDDDD             ", "            DD     DD            ",
                                            "            D  EEE  D            ", "       CCC  D EAAAE D  CCC       ",
                                            "          DDD EAAAE DDD          ", "       CCC  D EAAAE D  CCC       ",
                                            "            D  EEE  D            ", "            DD     DD            ",
                                            "             DDDDDDD             ", "                D                ",
                                            "                D                ", "               C C               ",
                                            "               C C               ", "               C C               ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "               C C               ", "               C C               ",
                                            "                D                ", "                D                ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "      CC                 CC      ",
                                            "        DD             DD        ", "      CC                 CC      ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                D                ",
                                            "                D                ", "               C C               ",
                                            "               C C               ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "               C C               ",
                                            "              CCCCC              ", "                D                ",
                                            "                A                ", "                A                ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "      C                   C      ", "     CC                   CC     ",
                                            "      CDAA             AADC      ", "     CC                   CC     ",
                                            "      C                   C      ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                A                ",
                                            "                A                ", "                D                ",
                                            "              CCCCC              ", "               C C               ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "               C C               ", "               C C               ",
                                            "                D                ", "             SEEAEES             ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "       S                 S       ",
                                            "       E                 E       ", "    CC E                 E CC    ",
                                            "      DA                 AD      ", "    CC E                 E CC    ",
                                            "       E                 E       ", "       S                 S       ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "             SEEAEES             ",
                                            "                D                ", "               C C               ",
                                            "               C C               ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "               C C               ",
                                            "              CCCCC              ", "                D                ",
                                            "                A                ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "    C                       C    ", "   CC                       CC   ",
                                            "    CDA                   ADC    ", "   CC                       CC   ",
                                            "    C                       C    ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                A                ", "                D                ",
                                            "              CCCCC              ", "               C C               ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "               C C               ", "               C C               ",
                                            "                D                ", "             SEEAEES             ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "     S                     S     ",
                                            "     E                     E     ", "  CC E                     E CC  ",
                                            "    DA                     AD    ", "  CC E                     E CC  ",
                                            "     E                     E     ", "     S                     S     ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "             SEEAEES             ",
                                            "                D                ", "               C C               ",
                                            "               C C               ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "               C C               ", "                D                ",
                                            "                A                ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "  C                           C  ",
                                            "   DA                       AD   ", "  C                           C  ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                A                ", "                D                ",
                                            "               C C               ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "               C C               ",
                                            "               C C               ", "                D                ",
                                            "                A                ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", " CC                           CC ",
                                            "   DA                       AD   ", " CC                           CC ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                A                ", "                D                ",
                                            "               C C               ", "               C C               ",
                                            "                                 " },
                                    { "                                 ", "               C C               ",
                                            "                D                ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", " C                             C ",
                                            "  D                           D  ", " C                             C ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                D                ", "               C C               ",
                                            "                                 " },
                                    { "                                 ", "               C C               ",
                                            "                D                ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", " C                             C ",
                                            "  D                           D  ", " C                             C ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                D                ", "               C C               ",
                                            "                                 " },
                                    { "             CCCCCCC             ", "               C C               ",
                                            "             DDDDDDD             ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "  D                           D  ",
                                            "  D                           D  ", "CCD                           DCC",
                                            "  D                           D  ", "CCD                           DCC",
                                            "  D                           D  ", "  D                           D  ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "             DDDDDDD             ", "               C C               ",
                                            "               C C               " },
                                    { "            CCHHHHHCC            ", "              DDDDD              ",
                                            "            DD     DD            ", "                                 ",
                                            "                                 ", "       S                 S       ",
                                            "                                 ", "     S                     S     ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "  D                           D  ", "  D                           D  ",
                                            " D                             D ", "CD                             DC",
                                            " D                             D ", "CD                             DC",
                                            " D                             D ", "  D                           D  ",
                                            "  D                           D  ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "     S                     S     ",
                                            "                                 ", "       S                 S       ",
                                            "                                 ", "                                 ",
                                            "            DD     DD            ", "              DDDDD              ",
                                            "               C C               " },
                                    { "            CHHHHHHHC            ", "             DDCDCDD             ",
                                            "            D  EEE  D            ", "                                 ",
                                            "      C                   C      ", "       E                 E       ",
                                            "    C                       C    ", "     E                     E     ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "  D                           D  ", " D                             D ",
                                            " D                             D ", "CCE                           ECC",
                                            " DE                           ED ", "CCE                           ECC",
                                            " D                             D ", " D                             D ",
                                            "  D                           D  ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "     E                     E     ",
                                            "    C                       C    ", "       E                 E       ",
                                            "      C                   C      ", "                                 ",
                                            "            D  EEE  D            ", "             DDCDCDD             ",
                                            "               C C               " },
                                    { "            CHHCCCHHC            ", "         CCCCDCCDCCDCCCC         ",
                                            "       CCC  D EAAAE D  CCC       ", "      CC                 CC      ",
                                            "     CC                   CC     ", "    CC E                 E CC    ",
                                            "   CC                       CC   ", "  CC E                     E CC  ",
                                            "  C                           C  ", " CC                           CC ",
                                            " C                             C ", " C                             C ",
                                            "CCD                           DCC", "CD                             DC",
                                            "CCE                           ECC", "CCA                           ACC",
                                            "CDA                           ADC", "CCA                           ACC",
                                            "CCE                           ECC", "CD                             DC",
                                            "CCD                           DCC", " C                             C ",
                                            " C                             C ", " CC                           CC ",
                                            "  C                           C  ", "  CC E                     E CC  ",
                                            "   CC                       CC   ", "    CC E                 E CC    ",
                                            "     CC                   CC     ", "      CC                 CC      ",
                                            "       CCC  D EAAAE D  CCC       ", "         CCCCDCCDCCDCCCC         ",
                                            "            CCCCCCCCC            " },
                                    { "            CHHC~CHHC            ", "             DDDDDDD             ",
                                            "          DDD EAAAE DDD          ", "        DD             DD        ",
                                            "      CDAA             AADC      ", "      DA                 AD      ",
                                            "    CDA                   ADC    ", "    DA                     AD    ",
                                            "   DA                       AD   ", "   DA                       AD   ",
                                            "  D                           D  ", "  D                           D  ",
                                            "  D                           D  ", " D                             D ",
                                            " DE                           ED ", "CDA                           ADC",
                                            " DA                           AD ", "CDA                           ADC",
                                            " DE                           ED ", " D                             D ",
                                            "  D                           D  ", "  D                           D  ",
                                            "  D                           D  ", "   DA                       AD   ",
                                            "   DA                       AD   ", "    DA                     AD    ",
                                            "    CDA                   ADC    ", "      DA                 AD      ",
                                            "      CDAA             AADC      ", "        DD             DD        ",
                                            "          DDD EAAAE DDD          ", "             DDDDDDD             ",
                                            "               C C               " },
                                    { "            CHHCCCHHC            ", "         CCCCDCCDCCDCCCC         ",
                                            "       CCC  D EAAAE D  CCC       ", "      CC                 CC      ",
                                            "     CC                   CC     ", "    CC E                 E CC    ",
                                            "   CC                       CC   ", "  CC E                     E CC  ",
                                            "  C                           C  ", " CC                           CC ",
                                            " C                             C ", " C                             C ",
                                            "CCD                           DCC", "CD                             DC",
                                            "CCE                           ECC", "CCA                           ACC",
                                            "CDA                           ADC", "CCA                           ACC",
                                            "CCE                           ECC", "CD                             DC",
                                            "CCD                           DCC", " C                             C ",
                                            " C                             C ", " CC                           CC ",
                                            "  C                           C  ", "  CC E                     E CC  ",
                                            "   CC                       CC   ", "    CC E                 E CC    ",
                                            "     CC                   CC     ", "      CC                 CC      ",
                                            "       CCC  D EAAAE D  CCC       ", "         CCCCDCCDCCDCCCC         ",
                                            "            CCCCCCCCC            " },
                                    { "            CHHHHHHHC            ", "             DDCDCDD             ",
                                            "            D  EEE  D            ", "                                 ",
                                            "      C                   C      ", "       E                 E       ",
                                            "    C                       C    ", "     E                     E     ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "  D                           D  ", " D                             D ",
                                            " D                             D ", "CCE                           ECC",
                                            " DE                           ED ", "CCE                           ECC",
                                            " D                             D ", " D                             D ",
                                            "  D                           D  ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "     E                     E     ",
                                            "    C                       C    ", "       E                 E       ",
                                            "      C                   C      ", "                                 ",
                                            "            D  EEE  D            ", "             DDCDCDD             ",
                                            "               C C               " },
                                    { "            CCHHHHHCC            ", "              DDDDD              ",
                                            "            DD     DD            ", "                                 ",
                                            "                                 ", "       S                 S       ",
                                            "                                 ", "     S                     S     ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "  D                           D  ", "  D                           D  ",
                                            " D                             D ", "CD                             DC",
                                            " D                             D ", "CD                             DC",
                                            " D                             D ", "  D                           D  ",
                                            "  D                           D  ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "     S                     S     ",
                                            "                                 ", "       S                 S       ",
                                            "                                 ", "                                 ",
                                            "            DD     DD            ", "              DDDDD              ",
                                            "               C C               " },
                                    { "             CCCCCCC             ", "               C C               ",
                                            "             DDDDDDD             ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "  D                           D  ",
                                            "  D                           D  ", "CCD                           DCC",
                                            "  D                           D  ", "CCD                           DCC",
                                            "  D                           D  ", "  D                           D  ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "             DDDDDDD             ", "               C C               ",
                                            "               C C               " },
                                    { "                                 ", "               C C               ",
                                            "                D                ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", " C                             C ",
                                            "  D                           D  ", " C                             C ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                D                ", "               C C               ",
                                            "                                 " },
                                    { "                                 ", "               C C               ",
                                            "                D                ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", " C                             C ",
                                            "  D                           D  ", " C                             C ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                D                ", "               C C               ",
                                            "                                 " },
                                    { "                                 ", "               C C               ",
                                            "               C C               ", "                D                ",
                                            "                A                ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", " CC                           CC ",
                                            "   DA                       AD   ", " CC                           CC ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                A                ", "                D                ",
                                            "               C C               ", "               C C               ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "               C C               ", "                D                ",
                                            "                A                ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "  C                           C  ",
                                            "   DA                       AD   ", "  C                           C  ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                A                ", "                D                ",
                                            "               C C               ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "               C C               ", "               C C               ",
                                            "                D                ", "             SEEAEES             ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "     S                     S     ",
                                            "     E                     E     ", "  CC E                     E CC  ",
                                            "    DA                     AD    ", "  CC E                     E CC  ",
                                            "     E                     E     ", "     S                     S     ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "             SEEAEES             ",
                                            "                D                ", "               C C               ",
                                            "               C C               ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "               C C               ",
                                            "              CCCCC              ", "                D                ",
                                            "                A                ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "    C                       C    ", "   CC                       CC   ",
                                            "    CDA                   ADC    ", "   CC                       CC   ",
                                            "    C                       C    ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                A                ", "                D                ",
                                            "              CCCCC              ", "               C C               ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "               C C               ", "               C C               ",
                                            "                D                ", "             SEEAEES             ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "       S                 S       ",
                                            "       E                 E       ", "    CC E                 E CC    ",
                                            "      DA                 AD      ", "    CC E                 E CC    ",
                                            "       E                 E       ", "       S                 S       ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "             SEEAEES             ",
                                            "                D                ", "               C C               ",
                                            "               C C               ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "               C C               ",
                                            "              CCCCC              ", "                D                ",
                                            "                A                ", "                A                ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "      C                   C      ", "     CC                   CC     ",
                                            "      CDAA             AADC      ", "     CC                   CC     ",
                                            "      C                   C      ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                A                ",
                                            "                A                ", "                D                ",
                                            "              CCCCC              ", "               C C               ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "               C C               ", "               C C               ",
                                            "                D                ", "                D                ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "      CC                 CC      ",
                                            "        DD             DD        ", "      CC                 CC      ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                D                ",
                                            "                D                ", "               C C               ",
                                            "               C C               ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "               C C               ",
                                            "               C C               ", "               C C               ",
                                            "                D                ", "                D                ",
                                            "             DDDDDDD             ", "            DD     DD            ",
                                            "            D  EEE  D            ", "       CCC  D EAAAE D  CCC       ",
                                            "          DDD EAAAE DDD          ", "       CCC  D EAAAE D  CCC       ",
                                            "            D  EEE  D            ", "            DD     DD            ",
                                            "             DDDDDDD             ", "                D                ",
                                            "                D                ", "               C C               ",
                                            "               C C               ", "               C C               ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "               C C               ",
                                            "               C C               ", "               C C               ",
                                            "               C C               ", "              DDDDD              ",
                                            "             DDCDCDD             ", "         CCCCDCCDCCDCCCC         ",
                                            "             DDDDDDD             ", "         CCCCDCCDCCDCCCC         ",
                                            "             DDCDCDD             ", "              DDDDD              ",
                                            "               C C               ", "               C C               ",
                                            "               C C               ", "               C C               ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " },
                                    { "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "               C C               ", "               C C               ",
                                            "               C C               ", "            CCCCCCCCC            ",
                                            "               C C               ", "            CCCCCCCCC            ",
                                            "               C C               ", "               C C               ",
                                            "               C C               ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 ", "                                 ",
                                            "                                 " } }))
            .addElement(
                    'A',
                    ofBlocksTiered(
                            (block, meta) -> block == TT_Container_Casings.SpacetimeCompressionFieldGenerators ? meta
                                    : null,
                            ImmutableList.of(
                                    Pair.of(TT_Container_Casings.SpacetimeCompressionFieldGenerators, 0),
                                    Pair.of(TT_Container_Casings.SpacetimeCompressionFieldGenerators, 1),
                                    Pair.of(TT_Container_Casings.SpacetimeCompressionFieldGenerators, 2),
                                    Pair.of(TT_Container_Casings.SpacetimeCompressionFieldGenerators, 3),
                                    Pair.of(TT_Container_Casings.SpacetimeCompressionFieldGenerators, 4),
                                    Pair.of(TT_Container_Casings.SpacetimeCompressionFieldGenerators, 5),
                                    Pair.of(TT_Container_Casings.SpacetimeCompressionFieldGenerators, 6),
                                    Pair.of(TT_Container_Casings.SpacetimeCompressionFieldGenerators, 7),
                                    Pair.of(TT_Container_Casings.SpacetimeCompressionFieldGenerators, 8)),
                            -1,
                            (t, meta) -> t.spacetimeCompressionFieldMetadata = meta,
                            t -> t.spacetimeCompressionFieldMetadata))
            .addElement(
                    'S',
                    ofBlocksTiered(
                            (block, meta) -> block == TT_Container_Casings.StabilisationFieldGenerators ? meta : null,
                            ImmutableList.of(
                                    Pair.of(TT_Container_Casings.StabilisationFieldGenerators, 0),
                                    Pair.of(TT_Container_Casings.StabilisationFieldGenerators, 1),
                                    Pair.of(TT_Container_Casings.StabilisationFieldGenerators, 2),
                                    Pair.of(TT_Container_Casings.StabilisationFieldGenerators, 3),
                                    Pair.of(TT_Container_Casings.StabilisationFieldGenerators, 4),
                                    Pair.of(TT_Container_Casings.StabilisationFieldGenerators, 5),
                                    Pair.of(TT_Container_Casings.StabilisationFieldGenerators, 6),
                                    Pair.of(TT_Container_Casings.StabilisationFieldGenerators, 7),
                                    Pair.of(TT_Container_Casings.StabilisationFieldGenerators, 8)),
                            -1,
                            (t, meta) -> t.stabilisationFieldMetadata = meta,
                            t -> t.stabilisationFieldMetadata))
            .addElement('C', ofBlock(sBlockCasingsBA0, 11)).addElement('D', ofBlock(sBlockCasingsBA0, 10))
            .addElement(
                    'H',
                    buildHatchAdder(GT_MetaTileEntity_EM_EyeOfHarmony.class)
                            .atLeast(InputHatch, OutputHatch, InputBus, OutputBus).casingIndex(texturePage << 7).dot(1)
                            .buildAndChain(sBlockCasingsBA0, 12))
            .addElement(
                    'E',
                    ofBlocksTiered(
                            (block, meta) -> block == TT_Container_Casings.TimeAccelerationFieldGenerator ? meta : null,
                            ImmutableList.of(
                                    Pair.of(TT_Container_Casings.TimeAccelerationFieldGenerator, 0),
                                    Pair.of(TT_Container_Casings.TimeAccelerationFieldGenerator, 1),
                                    Pair.of(TT_Container_Casings.TimeAccelerationFieldGenerator, 2),
                                    Pair.of(TT_Container_Casings.TimeAccelerationFieldGenerator, 3),
                                    Pair.of(TT_Container_Casings.TimeAccelerationFieldGenerator, 4),
                                    Pair.of(TT_Container_Casings.TimeAccelerationFieldGenerator, 5),
                                    Pair.of(TT_Container_Casings.TimeAccelerationFieldGenerator, 6),
                                    Pair.of(TT_Container_Casings.TimeAccelerationFieldGenerator, 7),
                                    Pair.of(TT_Container_Casings.TimeAccelerationFieldGenerator, 8)),
                            -1,
                            (t, meta) -> t.timeAccelerationFieldMetadata = meta,
                            t -> t.timeAccelerationFieldMetadata))
            .build();

    private double hydrogenOverflowProbabilityAdjustment;
    private double heliumOverflowProbabilityAdjustment;
    private static final long TICKS_BETWEEN_HATCH_DRAIN = EOH_DEBUG_MODE ? 10 : 20;

    private List<ItemStackLong> outputItems = new ArrayList<>();

    private void calculateHydrogenHeliumInputExcessValues(final long hydrogenRecipeRequirement,
            final long heliumRecipeRequirement) {

        double hydrogenStored = validFluidMap.get(Materials.Hydrogen.getGas(1L));
        double heliumStored = validFluidMap.get(Materials.Helium.getGas(1L));

        double hydrogenExcessPercentage = hydrogenStored / hydrogenRecipeRequirement - 1;
        double heliumExcessPercentage = heliumStored / heliumRecipeRequirement - 1;

        hydrogenOverflowProbabilityAdjustment = 1 - exp(-pow(30 * hydrogenExcessPercentage, 2));
        heliumOverflowProbabilityAdjustment = 1 - exp(-pow(30 * heliumExcessPercentage, 2));
    }

    private double recipeChanceCalculator() {
        double chance = currentRecipe.getBaseRecipeSuccessChance()
                - timeAccelerationFieldMetadata * TIME_ACCEL_DECREASE_CHANCE_PER_TIER
                + stabilisationFieldMetadata * STABILITY_INCREASE_PROBABILITY_DECREASE_YIELD_PER_TIER
                - hydrogenOverflowProbabilityAdjustment
                - heliumOverflowProbabilityAdjustment;

        return clamp(chance, 0.0, 1.0);
    }

    public static double clamp(double amount, double min, double max) {
        return Math.max(min, Math.min(amount, max));
    }

    private double recipeYieldCalculator() {
        double yield = 1.0 - hydrogenOverflowProbabilityAdjustment
                - heliumOverflowProbabilityAdjustment
                - stabilisationFieldMetadata * STABILITY_INCREASE_PROBABILITY_DECREASE_YIELD_PER_TIER;

        return clamp(yield, 0.0, 1.0);
    }

    private int recipeProcessTimeCalculator(final long recipeTime, final long recipeSpacetimeCasingRequired) {

        // Tier 1 recipe.
        // Tier 2 spacetime blocks.
        // = 3% discount.

        // Tier 1 recipe.
        // Tier 3 spacetime blocks.
        // = 3%*3% = 5.91% discount.

        final long spacetimeCasingDifference = (recipeSpacetimeCasingRequired - spacetimeCompressionFieldMetadata);
        final double recipeTimeDiscounted = recipeTime * pow(2.0, -timeAccelerationFieldMetadata)
                * pow(1 - SPACETIME_CASING_DIFFERENCE_DISCOUNT_PERCENTAGE, -spacetimeCasingDifference)
                / max(1, pow(2, currentCircuitMultiplier));
        return (int) Math.max(recipeTimeDiscounted, 1.0);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_EM_EyeOfHarmony> getStructure_EM() {
        return STRUCTURE_DEFINITION;
    }

    public GT_MetaTileEntity_EM_EyeOfHarmony(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_EM_EyeOfHarmony(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_EM_EyeOfHarmony(mName);
    }

    @Override
    public boolean checkMachine_EM(IGregTechTileEntity iGregTechTileEntity, ItemStack itemStack) {

        spacetimeCompressionFieldMetadata = -1;
        timeAccelerationFieldMetadata = -1;
        stabilisationFieldMetadata = -1;

        // Check structure of multi.
        if (!structureCheck_EM(STRUCTURE_PIECE_MAIN, 16, 16, 0)) {
            return false;
        }

        // Check if there is 1 output bus, and it is a ME output bus.
        {
            if (mOutputBusses.size() != 1) {
                return false;
            }

            if (!(mOutputBusses.get(0) instanceof GT_MetaTileEntity_Hatch_OutputBus_ME)) {
                return false;
            }
        }

        // Check if there is 1 output hatch, and they are ME output hatches.
        {
            if (mOutputHatches.size() != 1) {
                return false;
            }

            if (!(mOutputHatches.get(0) instanceof GT_MetaTileEntity_Hatch_Output_ME)) {
                return false;
            }
        }

        // Check there is 1 input bus.
        if (mInputBusses.size() != 1) {
            return false;
        }

        // Make sure there are no energy hatches.
        {
            if (mEnergyHatches.size() > 0) {
                return false;
            }

            if (mExoticEnergyHatches.size() > 0) {
                return false;
            }
        }

        // Make sure there are 2 input hatches.
        if (mInputHatches.size() != 2) {
            return false;
        }

        mHardHammer = true;
        mSoftHammer = true;
        mScrewdriver = true;
        mCrowbar = true;
        mSolderingTool = true;
        mWrench = true;
        return true;
    }

    private boolean animationsEnabled = true;

    public final void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        animationsEnabled = !animationsEnabled;
        aPlayer.addChatMessage(
                new ChatComponentText("Animations are now " + (animationsEnabled ? "enabled" : "disabled") + "."));
    }

    @Override
    public GT_Multiblock_Tooltip_Builder createTooltip() {
        final GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType("Spacetime Manipulator, EOH").addInfo(TOOLTIP_BAR)
                .addInfo("Creates a pocket of spacetime that is bigger on the inside using transdimensional")
                .addInfo("engineering. Certified Time Lord regulation compliant. This multi uses too much EU")
                .addInfo("to be handled with conventional means. All EU requirements are handled directly by")
                .addInfo("your wireless EU network.").addInfo(TOOLTIP_BAR)
                .addInfo("This multiblock will constantly consume hydrogen and helium when it is not running a")
                .addInfo("recipe once per second. It will store this internally, you can see the totals by")
                .addInfo("using a scanner. This multi also has three tiered blocks with " + RED + 9 + GRAY + " tiers")
                .addInfo("each. They are as follows and have the associated effects on the multi.")
                .addInfo(BLUE + "Spacetime Compression Field Generator:")
                .addInfo("- The tier of this block determines what recipes can be run. If the multiblocks")
                .addInfo("  spacetime compression field block exceeds the requirements of the recipe it")
                .addInfo(
                        "  will decrease the processing time by " + RED
                                + formatNumbers(SPACETIME_CASING_DIFFERENCE_DISCOUNT_PERCENTAGE * 100)
                                + "%"
                                + GRAY
                                + " per tier over the requirement (multiplicative).")
                .addInfo(BLUE + "Time Dilation Field Generator:")
                .addInfo(
                        "- Decreases the time required for a recipe by " + RED
                                + "50%"
                                + GRAY
                                + " per tier of block (multiplicative).")
                .addInfo(
                        "  Decreases the probability of a recipe succeeding by " + RED
                                + formatNumbers(TIME_ACCEL_DECREASE_CHANCE_PER_TIER * 100)
                                + "%"
                                + GRAY
                                + " per tier (additive)")
                .addInfo(BLUE + "Stabilisation Field Generator:")
                .addInfo(
                        "- Increases the power output and probability of a recipe succeeding by " + RED
                                + formatNumbers(STABILITY_INCREASE_PROBABILITY_DECREASE_YIELD_PER_TIER * 100)
                                + "%"
                                + GRAY
                                + " per tier (additive).")
                .addInfo(
                        "  Decreases the yield of a recipe by " + RED
                                + formatNumbers(STABILITY_INCREASE_PROBABILITY_DECREASE_YIELD_PER_TIER * 100)
                                + "%"
                                + GRAY
                                + " per tier (additive). ")
                .addInfo(TOOLTIP_BAR)
                .addInfo(
                        "Going over a recipe requirement on hydrogen or helium has a penalty on yield and recipe chance.")
                .addInfo("All stored hydrogen and helium is consumed during a craft. The associated formulas are:")
                .addInfo(GREEN + "percentageOverflow = fluidStored/recipeRequirement - 1")
                .addInfo(GREEN + "adjustmentValue = 1 - exp(-(30 * percentageOverflow)^2)")
                .addInfo("The value of adjustmentValue is then subtracted from the total yield and recipe chance.")
                .addInfo(TOOLTIP_BAR)
                .addInfo("It should be noted that base recipe chance is determined per recipe and yield always starts")
                .addInfo("at 1 and subtracts depending on penalties. All fluid/item outputs are multiplied by the")
                .addInfo("yield. Failure fluid is exempt. All power outputs start at 60% of what the recipes display.")
                .addInfo(TOOLTIP_BAR)
                .addInfo("This multiblock can only output to ME output buses/hatches. If no space in the network")
                .addInfo(
                        "is available the items/fluids will be " + UNDERLINE + DARK_RED + "voided" + RESET + GRAY + ".")
                .addInfo(TOOLTIP_BAR)
                .addInfo("This multiblock can be overclocked by placing a programmed circuit into the input bus.")
                .addInfo("Each OC halves recipe time and multiplies startup cost by a factor of:")
                .addInfo(GREEN + "(log4.4(overclockAmount + 1) + 1) * 0.77^overclockAmount")
                .addInfo(
                        "Furthermore, each OC decreases the power output by a factor of " + RED
                                + "0.77^overclockAmount"
                                + GRAY
                                + ".")
                .addInfo("All item and fluid output chances & amounts per recipe are unaffected.").addInfo(TOOLTIP_BAR)
                .addInfo(
                        "If a recipe fails the EOH will output " + GREEN
                                + "successChance * "
                                + formatNumbers(MOLTEN_SPACETIME_PER_FAILURE_TIER)
                                + " * ("
                                + SPACETIME_FAILURE_BASE
                                + ")^(Recipe tier)"
                                + GRAY
                                + "L of molten")
                .addInfo(
                        MaterialsUEVplus.SpaceTime.getLocalizedNameForItem("%material")
                                + " instead of fluid/item outputs and output as much EU as a successful recipe.")
                .addInfo(TOOLTIP_BAR).addInfo("Animations can be disabled by using a screwdriver on the multiblock.")
                .addSeparator().addStructureInfo("Eye of Harmony structure is too complex! See schematic for details.")
                .addStructureInfo(
                        EnumChatFormatting.GOLD + "896"
                                + EnumChatFormatting.GRAY
                                + " Reinforced Spatial Structure Casing.")
                .addStructureInfo(
                        EnumChatFormatting.GOLD + "534"
                                + EnumChatFormatting.GRAY
                                + " Reinforced Temporal Structure Casing.")
                .addStructureInfo(
                        EnumChatFormatting.GOLD + "31"
                                + EnumChatFormatting.GRAY
                                + " Infinite SpaceTime Energy Boundary Casing.")
                .addStructureInfo(
                        EnumChatFormatting.GOLD + "168" + EnumChatFormatting.GRAY + " Time Dilation Field Generator.")
                .addStructureInfo(
                        EnumChatFormatting.GOLD + "48" + EnumChatFormatting.GRAY + " Stabilisation Field Generator.")
                .addStructureInfo(
                        EnumChatFormatting.GOLD + "138"
                                + EnumChatFormatting.GRAY
                                + " Spacetime Compression Field Generator.")
                .addStructureInfo("--------------------------------------------")
                .addStructureInfo(
                        "Requires " + EnumChatFormatting.GOLD + 2 + EnumChatFormatting.GRAY + " input hatches.")
                .addStructureInfo(
                        "Requires " + EnumChatFormatting.GOLD + 1 + EnumChatFormatting.GRAY + " ME output hatch.")
                .addStructureInfo("Requires " + EnumChatFormatting.GOLD + 1 + EnumChatFormatting.GRAY + " input bus.")
                .addStructureInfo(
                        "Requires " + EnumChatFormatting.GOLD + 1 + EnumChatFormatting.GRAY + " ME output bus.")
                .addStructureInfo("--------------------------------------------").beginStructureBlock(33, 33, 33, false)
                .toolTipFinisher(AuthorColen.substring(8) + EnumChatFormatting.GRAY + "&" + CommonValues.TEC_MARK_EM);
        return tt;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister aBlockIconRegister) {
        ScreenOFF = new Textures.BlockIcons.CustomIcon("iconsets/EM_BHG");
        ScreenON = new Textures.BlockIcons.CustomIcon("iconsets/EM_BHG_ACTIVE");
        super.registerIcons(aBlockIconRegister);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
            int colorIndex, boolean aActive, boolean aRedstone) {
        if (side == facing) {
            return new ITexture[] { Textures.BlockIcons.casingTexturePages[texturePage][12],
                    new TT_RenderedExtendedFacingTexture(aActive ? ScreenON : ScreenOFF) };
        }
        return new ITexture[] { Textures.BlockIcons.casingTexturePages[texturePage][12] };
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        structureBuild_EM(STRUCTURE_PIECE_MAIN, 16, 16, 0, stackSize, hintsOnly);
    }

    private final Map<FluidStack, Long> validFluidMap = new HashMap<>() {

        private static final long serialVersionUID = -8452610443191188130L;

        {
            put(Materials.Hydrogen.getGas(1), 0L);
            put(Materials.Helium.getGas(1), 0L);
        }
    };

    private void drainFluidFromHatchesAndStoreInternally() {
        for (GT_MetaTileEntity_Hatch_Input inputHatch : mInputHatches) {
            FluidStack fluidInHatch = inputHatch.getFluid();

            if (fluidInHatch == null) {
                continue;
            }

            // Iterate over valid fluids and store them in a hashmap.
            for (FluidStack validFluid : validFluidMap.keySet()) {
                if (fluidInHatch.isFluidEqual(validFluid)) {
                    validFluidMap.put(validFluid, validFluidMap.get(validFluid) + (long) fluidInHatch.amount);
                    inputHatch.setFillableStack(null);
                }
            }
        }
    }

    private EyeOfHarmonyRecipe currentRecipe;

    // Counter for lag prevention.
    private long lagPreventer = 0;

    // Check for recipe every recipeCheckInterval ticks.
    private static final long RECIPE_CHECK_INTERVAL = 3 * 20;
    private long currentCircuitMultiplier = 0;

    @Override
    @NotNull
    protected CheckRecipeResult checkProcessing_EM() {
        ItemStack controllerStack = getControllerSlot();
        if (controllerStack == null) {
            return SimpleCheckRecipeResult.ofFailure("no_planet_block");
        }

        lagPreventer++;
        if (lagPreventer < RECIPE_CHECK_INTERVAL) {
            lagPreventer = 0;
            // No item in multi gui slot.

            currentRecipe = eyeOfHarmonyRecipeStorage.recipeLookUp(controllerStack);
            if (currentRecipe == null) {
                return CheckRecipeResultRegistry.NO_RECIPE;
            }
            CheckRecipeResult result = processRecipe(currentRecipe);

            if (result.wasSuccessful()) {
                return result;
            }

            currentRecipe = null;
        }
        return CheckRecipeResultRegistry.NO_RECIPE;
    }

    private long getHydrogenStored() {
        return validFluidMap.get(Materials.Hydrogen.getGas(1));
    }

    private long getHeliumStored() {
        return validFluidMap.get(Materials.Helium.getGas(1));
    }

    public CheckRecipeResult processRecipe(EyeOfHarmonyRecipe recipeObject) {

        // Get circuit damage, clamp it and then use it later for overclocking.
        ItemStack circuit = mInputBusses.get(0).getStackInSlot(0);
        if (circuit != null) {
            currentCircuitMultiplier = (long) clamp(circuit.getItemDamage(), 0, 24);
        } else {
            currentCircuitMultiplier = 0;
        }

        // Debug mode, overwrites the required fluids to initiate the recipe to 100L of each.
        if ((EOH_DEBUG_MODE && getHydrogenStored() < 100)
                || (getHydrogenStored() < currentRecipe.getHydrogenRequirement())) {
            return SimpleCheckRecipeResult.ofFailure("no_hydrogen");
        }
        if ((EOH_DEBUG_MODE && getHeliumStored() < 100) || (getHeliumStored() < currentRecipe.getHeliumRequirement())) {
            return SimpleCheckRecipeResult.ofFailure("no_helium");
        }

        if (spacetimeCompressionFieldMetadata == -1) {
            return CheckRecipeResultRegistry
                    .insufficientMachineTier((int) recipeObject.getSpacetimeCasingTierRequired());
        }

        // Check tier of spacetime compression blocks is high enough.
        if ((spacetimeCompressionFieldMetadata + 1) < recipeObject.getSpacetimeCasingTierRequired()) {
            return CheckRecipeResultRegistry
                    .insufficientMachineTier((int) recipeObject.getSpacetimeCasingTierRequired());
        }

        startEU = recipeObject.getEUStartCost();

        // Remove EU from the users network.
        long usedEU = (long) (-startEU * (Math.log(currentCircuitMultiplier + 1) / LOG_BASE_CONSTANT + 1)
                * pow(0.77, currentCircuitMultiplier));
        if (!addEUToGlobalEnergyMap(userUUID, usedEU)) {
            return CheckRecipeResultRegistry.insufficientPower(usedEU);
        }

        mMaxProgresstime = recipeProcessTimeCalculator(
                recipeObject.getRecipeTimeInTicks(),
                recipeObject.getSpacetimeCasingTierRequired());

        calculateHydrogenHeliumInputExcessValues(
                recipeObject.getHydrogenRequirement(),
                recipeObject.getHeliumRequirement());

        if (EOH_DEBUG_MODE) {
            hydrogenOverflowProbabilityAdjustment = 0;
            heliumOverflowProbabilityAdjustment = 0;
        }

        successChance = recipeChanceCalculator();
        currentRecipeRocketTier = currentRecipe.getRocketTier();

        // Determine EU recipe output.
        euOutput = (long) (recipeObject.getEUOutput() * pow(0.77, currentCircuitMultiplier));

        // Reduce internal storage by hydrogen and helium quantity required for recipe.
        validFluidMap.put(Materials.Hydrogen.getGas(1), 0L);
        validFluidMap.put(Materials.Helium.getGas(1), 0L);

        double yield = recipeYieldCalculator();
        if (EOH_DEBUG_MODE) {
            successChance = 1; // Debug recipes, sets them to 100% output chance.
        }

        // Return copies of the output objects.
        mOutputFluids = recipeObject.getOutputFluids();
        outputItems = recipeObject.getOutputItems();

        if (yield != 1.0) {
            // Iterate over item output list and apply yield values.
            for (ItemStackLong itemStackLong : outputItems) {
                itemStackLong.stackSize *= yield;
            }

            // Iterate over fluid output list and apply yield values.
            for (FluidStack fluidStack : mOutputFluids) {
                fluidStack.amount *= yield;
            }
        }

        updateSlots();

        if (animationsEnabled) {
            createRenderBlock(currentRecipe);
        }

        recipeRunning = true;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private void createRenderBlock(final EyeOfHarmonyRecipe currentRecipe) {

        IGregTechTileEntity gregTechTileEntity = this.getBaseMetaTileEntity();

        int x = gregTechTileEntity.getXCoord();
        int y = gregTechTileEntity.getYCoord();
        int z = gregTechTileEntity.getZCoord();

        double xOffset = 16 * getExtendedFacing().getRelativeBackInWorld().offsetX;
        double zOffset = 16 * getExtendedFacing().getRelativeBackInWorld().offsetZ;
        double yOffset = 16 * getExtendedFacing().getRelativeBackInWorld().offsetY;

        this.getBaseMetaTileEntity().getWorld()
                .setBlock((int) (x + xOffset), (int) (y + yOffset), (int) (z + zOffset), Blocks.air);
        this.getBaseMetaTileEntity().getWorld()
                .setBlock((int) (x + xOffset), (int) (y + yOffset), (int) (z + zOffset), eyeOfHarmonyRenderBlock);
        TileEyeOfHarmony rendererTileEntity = (TileEyeOfHarmony) this.getBaseMetaTileEntity().getWorld()
                .getTileEntity((int) (x + xOffset), (int) (y + yOffset), (int) (z + zOffset));

        rendererTileEntity.setTier(currentRecipe.getRocketTier());

        int recipeSpacetimeTier = (int) currentRecipe.getSpacetimeCasingTierRequired();

        // Star is a larger size depending on the spacetime tier of the recipe.
        rendererTileEntity.setSize((1 + recipeSpacetimeTier));

        // Star rotates faster the higher tier time dilation you use in the multi.
        // Lower value = faster rotation speed.
        rendererTileEntity.setRotationSpeed((1 + timeAccelerationFieldMetadata) / 2.0f);
    }

    private double successChance;
    private long currentRecipeRocketTier;

    private void outputFailedChance() {
        // 2^Tier spacetime released upon recipe failure.
        mOutputFluids = new FluidStack[] { MaterialsUEVplus.SpaceTime.getMolten(
                (long) (successChance * MOLTEN_SPACETIME_PER_FAILURE_TIER
                        * pow(SPACETIME_FAILURE_BASE, currentRecipeRocketTier + 1))) };
        super.outputAfterRecipe_EM();
    }

    @Override
    public void stopMachine() {
        super.stopMachine();
        recipeRunning = false;
    }

    private void destroyRenderBlock() {
        IGregTechTileEntity gregTechTileEntity = this.getBaseMetaTileEntity();

        int x = gregTechTileEntity.getXCoord();
        int y = gregTechTileEntity.getYCoord();
        int z = gregTechTileEntity.getZCoord();

        double xOffset = 16 * getExtendedFacing().getRelativeBackInWorld().offsetX;
        double zOffset = 16 * getExtendedFacing().getRelativeBackInWorld().offsetZ;
        double yOffset = 16 * getExtendedFacing().getRelativeBackInWorld().offsetY;

        this.getBaseMetaTileEntity().getWorld()
                .setBlock((int) (x + xOffset), (int) (y + yOffset), (int) (z + zOffset), Blocks.air);
    }

    public void outputAfterRecipe_EM() {
        recipeRunning = false;
        eRequiredData = 0L;

        destroyRenderBlock();

        // Output power with stabilization factor (5% loss per tier below gallifreyan)
        addEUToGlobalEnergyMap(
                userUUID,
                (long) (euOutput * (1 - ((TOTAL_CASING_TIERS_WITH_POWER_PENALTY - stabilisationFieldMetadata)
                        * STABILITY_INCREASE_PROBABILITY_DECREASE_YIELD_PER_TIER))));
        startEU = 0;
        euOutput = 0;

        if (successChance < random()) {
            outputFailedChance();
            outputItems = new ArrayList<>();
            return;
        }

        for (ItemStackLong itemStack : outputItems) {
            outputItemToAENetwork(itemStack.itemStack, itemStack.stackSize);
        }

        // Clear the array list for new recipes.
        outputItems = new ArrayList<>();

        // Do other stuff from TT superclasses. E.g. outputting fluids.
        super.outputAfterRecipe_EM();
    }

    @Override
    public void onPreTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPreTick(aBaseMetaTileEntity, aTick);

        if (aTick == 1) {
            userUUID = String.valueOf(getBaseMetaTileEntity().getOwnerUuid());
            String userName = getBaseMetaTileEntity().getOwnerName();
            strongCheckOrAddUser(userUUID, userName);
        }

        if (!recipeRunning) {
            if ((aTick % TICKS_BETWEEN_HATCH_DRAIN) == 0) {
                drainFluidFromHatchesAndStoreInternally();
            }
        }
    }

    private boolean recipeRunning = false;

    // Will void if AE network is full.
    private void outputItemToAENetwork(ItemStack item, long amount) {

        if ((item == null) || (amount <= 0)) {
            return;
        }

        if (amount < Integer.MAX_VALUE) {
            ItemStack tmpItem = item.copy();
            tmpItem.stackSize = (int) amount;
            ((GT_MetaTileEntity_Hatch_OutputBus_ME) mOutputBusses.get(0)).store(tmpItem);
        } else {
            // For item stacks > Int max.
            while (amount >= Integer.MAX_VALUE) {
                ItemStack tmpItem = item.copy();
                tmpItem.stackSize = Integer.MAX_VALUE;
                ((GT_MetaTileEntity_Hatch_OutputBus_ME) mOutputBusses.get(0)).store(tmpItem);
                amount -= Integer.MAX_VALUE;
            }
            ItemStack tmpItem = item.copy();
            tmpItem.stackSize = (int) amount;
            ((GT_MetaTileEntity_Hatch_OutputBus_ME) mOutputBusses.get(0)).store(tmpItem);
        }
    }

    @Override
    public String[] getInfoData() {
        ArrayList<String> str = new ArrayList<>(Arrays.asList(super.getInfoData()));
        str.add(GOLD + "---------------- Control Block Statistics ----------------");
        str.add("Spacetime Compression Field Grade: " + BLUE + spacetimeCompressionFieldMetadata);
        str.add("Time Dilation Field Grade: " + BLUE + timeAccelerationFieldMetadata);
        str.add("Stabilisation Field Grade: " + BLUE + stabilisationFieldMetadata);
        str.add(GOLD + "----------------- Internal Fluids Stored ----------------");
        validFluidMap.forEach(
                (key, value) -> str.add(BLUE + key.getLocalizedName() + RESET + " : " + RED + formatNumbers(value)));
        if (recipeRunning) {
            str.add(GOLD + "---------------------- Other Stats ---------------");
            str.add("Recipe Success Chance: " + RED + formatNumbers(100 * successChance) + RESET + "%");
            str.add("Recipe Yield: " + RED + formatNumbers(100 * recipeYieldCalculator()) + RESET + "%");
            str.add(
                    "EU Output: " + RED
                            + formatNumbers(
                                    euOutput * (1
                                            - ((TOTAL_CASING_TIERS_WITH_POWER_PENALTY - stabilisationFieldMetadata)
                                                    * STABILITY_INCREASE_PROBABILITY_DECREASE_YIELD_PER_TIER)))
                            + RESET
                            + " EU");
            if (mOutputFluids.length > 0) {
                // Star matter is always the last element in the array.
                str.add(
                        "Estimated Star Matter Output: " + RED
                                + formatNumbers(mOutputFluids[mOutputFluids.length - 1].amount)
                                + RESET
                                + " L");
            }
            long euPerTick = (long) (startEU * (Math.log(currentCircuitMultiplier + 1) / LOG_BASE_CONSTANT + 1)
                    * pow(0.77, currentCircuitMultiplier)
                    - euOutput * (1 - ((TOTAL_CASING_TIERS_WITH_POWER_PENALTY - stabilisationFieldMetadata)
                            * STABILITY_INCREASE_PROBABILITY_DECREASE_YIELD_PER_TIER)))
                    / -maxProgresstime();
            if (abs(euPerTick) < LongMath.pow(10, 12)) {
                str.add("Estimated EU/t: " + RED + formatNumbers(euPerTick) + RESET + " EU/t");
            } else {
                str.add(
                        "Estimated EU/t: " + RED
                                + ReadableNumberConverter.INSTANCE.toWideReadableForm(euPerTick)
                                + RESET
                                + " EU/t");
            }
            str.add(GOLD + "-----------------------------------------------------");
        }
        return str.toArray(new String[0]);
    }

    @Override
    public String[] getStructureDescription(ItemStack stackSize) {
        return new String[] { "Eye of Harmony multiblock" };
    }

    // NBT save/load strings.
    private static final String EYE_OF_HARMONY = "eyeOfHarmonyOutput";
    private static final String NUMBER_OF_ITEMS_NBT_TAG = EYE_OF_HARMONY + "numberOfItems";
    private static final String ITEM_OUTPUT_NBT_TAG = EYE_OF_HARMONY + "itemOutput";
    private static final String RECIPE_RUNNING_NBT_TAG = EYE_OF_HARMONY + "recipeRunning";
    private static final String RECIPE_EU_OUTPUT_NBT_TAG = EYE_OF_HARMONY + "euOutput";
    private static final String RECIPE_SUCCESS_CHANCE_NBT_TAG = EYE_OF_HARMONY + "recipeSuccessChance";
    private static final String ROCKET_TIER_NBT_TAG = EYE_OF_HARMONY + "rocketTier";
    private static final String CURRENT_CIRCUIT_MULTIPLIER_TAG = EYE_OF_HARMONY + "currentCircuitMultiplier";
    private static final String ANIMATIONS_ENABLED = EYE_OF_HARMONY + "animationsEnabled";

    // Sub tags, less specific names required.
    private static final String STACK_SIZE = "stackSize";
    private static final String ITEM_STACK_NBT_TAG = "itemStack";

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        // Save the quantity of fluid stored inside the controller.
        validFluidMap.forEach((key, value) -> aNBT.setLong("stored." + key.getUnlocalizedName(), value));

        aNBT.setBoolean(RECIPE_RUNNING_NBT_TAG, recipeRunning);
        aNBT.setLong(RECIPE_EU_OUTPUT_NBT_TAG, euOutput);
        aNBT.setDouble(RECIPE_SUCCESS_CHANCE_NBT_TAG, successChance);
        aNBT.setLong(ROCKET_TIER_NBT_TAG, currentRecipeRocketTier);
        aNBT.setLong(CURRENT_CIRCUIT_MULTIPLIER_TAG, currentCircuitMultiplier);
        aNBT.setBoolean(ANIMATIONS_ENABLED, animationsEnabled);

        // Store damage values/stack sizes of GT items being outputted.
        NBTTagCompound itemStackListNBTTag = new NBTTagCompound();
        itemStackListNBTTag.setLong(NUMBER_OF_ITEMS_NBT_TAG, outputItems.size());

        int index = 0;
        for (ItemStackLong itemStackLong : outputItems) {
            // Save stack size to NBT.
            itemStackListNBTTag.setLong(index + STACK_SIZE, itemStackLong.stackSize);

            // Save ItemStack to NBT.
            aNBT.setTag(index + ITEM_STACK_NBT_TAG, itemStackLong.itemStack.writeToNBT(new NBTTagCompound()));

            index++;
        }

        aNBT.setTag(ITEM_OUTPUT_NBT_TAG, itemStackListNBTTag);

        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(final NBTTagCompound aNBT) {

        // Load the quantity of fluid stored inside the controller.
        validFluidMap
                .forEach((key, value) -> validFluidMap.put(key, aNBT.getLong("stored." + key.getUnlocalizedName())));

        // Load other stuff from NBT.
        recipeRunning = aNBT.getBoolean(RECIPE_RUNNING_NBT_TAG);
        euOutput = aNBT.getLong(RECIPE_EU_OUTPUT_NBT_TAG);
        successChance = aNBT.getDouble(RECIPE_SUCCESS_CHANCE_NBT_TAG);
        currentRecipeRocketTier = aNBT.getLong(ROCKET_TIER_NBT_TAG);
        currentCircuitMultiplier = aNBT.getLong(CURRENT_CIRCUIT_MULTIPLIER_TAG);
        animationsEnabled = aNBT.getBoolean(ANIMATIONS_ENABLED);

        // Load damage values/stack sizes of GT items being outputted and convert back to items.
        NBTTagCompound tempItemTag = aNBT.getCompoundTag(ITEM_OUTPUT_NBT_TAG);

        // Iterate over all stored items.
        for (int index = 0; index < tempItemTag.getInteger(NUMBER_OF_ITEMS_NBT_TAG); index++) {

            // Load stack size from NBT.
            long stackSize = tempItemTag.getLong(index + STACK_SIZE);

            // Load ItemStack from NBT.
            ItemStack itemStack = ItemStack.loadItemStackFromNBT(aNBT.getCompoundTag(index + ITEM_STACK_NBT_TAG));

            outputItems.add(new ItemStackLong(itemStack, stackSize));
        }

        super.loadNBTData(aNBT);
    }
}
