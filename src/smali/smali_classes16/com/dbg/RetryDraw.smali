.class public final Lcom/dbg/RetryDraw;
.super Ljava/lang/Object;
.implements Lkotlin/jvm/functions/Function0;

.field public a:Lcom/tvsm/connect/feature/heremaps/presentation/fragments/HereMapsFragment;
.field public b:Ljava/util/ArrayList;
.field public c:Ljava/util/ArrayList;
.field public d:I
.field public e:I
.field public f:I
.field public g:Z

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public invoke()Ljava/lang/Object;
    .locals 7

    iget-object v0, p0, Lcom/dbg/RetryDraw;->a:Lcom/tvsm/connect/feature/heremaps/presentation/fragments/HereMapsFragment;

    iget-object v1, p0, Lcom/dbg/RetryDraw;->b:Ljava/util/ArrayList;

    iget-object v2, p0, Lcom/dbg/RetryDraw;->c:Ljava/util/ArrayList;

    iget v3, p0, Lcom/dbg/RetryDraw;->d:I

    iget v4, p0, Lcom/dbg/RetryDraw;->e:I

    iget v5, p0, Lcom/dbg/RetryDraw;->f:I

    iget-boolean v6, p0, Lcom/dbg/RetryDraw;->g:Z

    invoke-virtual/range {v0 .. v6}, Lcom/tvsm/connect/feature/heremaps/presentation/fragments/HereMapsFragment;->drawRoute(Ljava/util/ArrayList;Ljava/util/ArrayList;IIIZ)V

    sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;

    return-object v0
.end method
